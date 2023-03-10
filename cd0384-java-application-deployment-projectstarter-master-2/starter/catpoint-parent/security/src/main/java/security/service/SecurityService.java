package security.service;

import com.image.ImageService;
import security.model.AlarmStatus;
import security.model.ArmingStatus;
import security.model.SecurityRepository;
import security.model.Sensor;
import security.view.StatusListener;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static security.model.AlarmStatus.*;
import static security.model.ArmingStatus.*;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {
    private boolean catWasDetected;
    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
        this.catWasDetected = false;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == DISARMED) {
            setAlarmStatus(NO_ALARM);
        } else if ((armingStatus == ARMED_AWAY || armingStatus == ARMED_HOME) && (catWasDetected == false)) {
            getSensors().forEach(sensor -> {
                sensor.setActive(false);
            });
        } else if (catWasDetected == true && armingStatus == ARMED_HOME) {
            setAlarmStatus(ALARM);
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(cat && getArmingStatus() == ARMED_HOME) {
            setAlarmStatus(ALARM);
            getSensors().forEach(sensor -> {
                sensor.setActive(false);
            });
        } else {
            setAlarmStatus(NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM:
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
                break;
            case PENDING_ALARM:
                setAlarmStatus(ALARM);
                break;
            default:
                setAlarmStatus(NO_ALARM);
                break;
        }
    }


    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        AlarmStatus alarmStatus = securityRepository.getAlarmStatus();
        Set<Sensor> sensors = securityRepository.getSensors();
        if (sensors.stream().allMatch(s -> s.getActive() == false)) {
            if (alarmStatus == PENDING_ALARM) {
                securityRepository.setAlarmStatus(NO_ALARM);
            }
            return;
        }
        switch(alarmStatus) {
            case PENDING_ALARM:
                setAlarmStatus(NO_ALARM);
                break;
            case ALARM:
                break;
            case NO_ALARM:
                setAlarmStatus(AlarmStatus.PENDING_ALARM);
                break;
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        Boolean sensorActive = sensor.getActive();
        if(!sensorActive) {
            if (active) {
                handleSensorActivated();
            } else {
                handleSensorDeactivated();
            }
        } else if (sensorActive && !active){
            handleSensorDeactivated();
        } else if (sensorActive && active) {
            handleSensorActivated();
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        this.catWasDetected = imageService.imageContainsCat(currentCameraImage, 50.0f);
        catDetected(catWasDetected);
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    public boolean isCatWasDetected() {
        return catWasDetected;
    }

    public void setCatWasDetected(boolean catWasDetected) {
        this.catWasDetected = catWasDetected;
    }
}
