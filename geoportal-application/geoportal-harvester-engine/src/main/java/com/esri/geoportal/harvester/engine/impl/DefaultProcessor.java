/*
 * Copyright 2016 Esri, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.geoportal.harvester.engine.impl;

import com.esri.geoportal.harvester.api.DataReference;
import com.esri.geoportal.harvester.api.ProcessInstance;
import com.esri.geoportal.harvester.api.Processor;
import com.esri.geoportal.harvester.api.defs.EntityDefinition;
import com.esri.geoportal.harvester.api.defs.Task;
import com.esri.geoportal.harvester.api.ex.DataInputException;
import com.esri.geoportal.harvester.api.ex.DataOutputException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultProcessor.
 */
public class DefaultProcessor implements Processor {
  public static final String TYPE = "DEFAULT";

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProcessor.class);

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public EntityDefinition getEntityDefinition() {
    EntityDefinition entityDefiniton = new EntityDefinition();
    entityDefiniton.setType(TYPE);
    entityDefiniton.setLabel(TYPE);
    return entityDefiniton;
  }

  @Override
  public ProcessInstance createProcess(Task task) {
    LOG.info(String.format("SUBMITTING: %s", task));
    return new DefaultProcess(task);
  }

  /**
   * Default process.
   */
  public static class DefaultProcess implements ProcessInstance {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultProcess.class);
    private final List<ProcessInstance.Listener> listeners = Collections.synchronizedList(new ArrayList<>());

    final Task task;
    final Thread thread;

    private volatile boolean completed;
    private volatile boolean aborting;

    /**
     * Creates instance of the process.
     * @param task task
     */
    public DefaultProcess(Task task) {
      this.task = task;
      this.thread = new Thread(() -> {
        onStatusChange();
        LOG.info(String.format("Started harvest: %s", getTitle()));
        try {
          if (!task.getDataDestinations().isEmpty()) {
            while (task.getDataSource().hasNext()) {
              if (Thread.currentThread().isInterrupted()) {
                break;
              }
              DataReference dataReference = task.getDataSource().next();
              task.getDataDestinations().stream().forEach((d) -> {
                try {
                  d.publish(dataReference);
                  LOG.debug(String.format("Harvested %s during %s", dataReference, getTitle()));
                  onSuccess(dataReference);
                } catch (DataOutputException ex) {
                  LOG.warn(String.format("Failed harvesting %s during %s", dataReference, getTitle()));
                  onError(ex);
                }
              });
            }
          }
        } catch (DataInputException ex) {
          LOG.error(String.format("Error harvesting of %s", getTitle()), ex);
          onError(ex);
        } finally {
          completed = true;
          aborting = false;
          LOG.info(String.format("Completed harvest: %s", getTitle()));
          onStatusChange();
        }
      }, "HARVESTING");
      onStatusChange();
    }

    @Override
    public Task getTask() {
      return task;
    }

    @Override
    public void addListener(ProcessInstance.Listener listener) {
      listeners.add(listener);
    }

    /**
     * Gets process title.
     *
     * @return process title
     */
    @Override
    public String getTitle() {
      return String.format("%s --> %s", task.getDataSource().toString(), task.getDataDestinations());
    }

    /**
     * Gets process status.
     *
     * @return process status
     */
    @Override
    public synchronized ProcessInstance.Status getStatus() {
      if (completed) {
        return ProcessInstance.Status.completed;
      }
      if (aborting) {
        return ProcessInstance.Status.aborting;
      }
      if (thread.isAlive()) {
        return ProcessInstance.Status.working;
      }
      return ProcessInstance.Status.submitted;
    }

    /**
     * Begins the process.
     */
    @Override
    public synchronized void begin() {
      if (getStatus() != ProcessInstance.Status.submitted) {
        throw new IllegalStateException(String.format("Error begininig the process: process is in %s state", getStatus()));
      }
      thread.start();
    }

    /**
     * Aborts the process.
     */
    @Override
    public synchronized void abort() {
      if (getStatus() != ProcessInstance.Status.working) {
        throw new IllegalStateException(String.format("Error aborting the process: process is in %s state", getStatus()));
      }
      LOG.info(String.format("Aborting process: %s", getTitle()));
      aborting = true;
      thread.interrupt();
      onStatusChange();
    }

    /**
     * Called to handle output error.
     * @param ex output exception
     */
    private void onError(DataOutputException ex) {
      listeners.forEach(l -> {
        l.onError(ex);
      });
    }

    /**
     * Called to handle input error.
     * @param ex input exception
     */
    private void onError(DataInputException ex) {
      listeners.forEach(l -> l.onError(ex));
    }

    /**
     * Called to handle successful data processing
     * @param dataRef data reference
     */
    private void onSuccess(DataReference dataRef) {
      listeners.forEach(l -> l.onDataProcessed(dataRef));
    }
    
    /**
     * Called when status has been changed.
     */
    private void onStatusChange() {
      Status status = getStatus();
      listeners.forEach(l -> l.onStatusChange(status));
    }

    @Override
    public String toString() {
      return String.format("PROCESS:: status: %s, title: %s", getStatus(), getTitle());
    }
  }
}