package io.ampool.presto.log;

import io.airlift.log.Logger;

/**
 * Created by deepak on 2/2/18.
 */
public class AmpoolLogger {
  private final Logger logger;

  public AmpoolLogger(Class className) {
    logger = Logger.get(className);
  }

  public static AmpoolLogger get(Class className) {
    return new AmpoolLogger(className);
  }

  public void info(String message){
//    logger.info(message);
  }

  public void debug(String message) {
    logger.info(message);
  }
}
