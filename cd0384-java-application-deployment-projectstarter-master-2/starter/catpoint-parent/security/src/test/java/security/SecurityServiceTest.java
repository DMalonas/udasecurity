package security;

import com.image.ImageService;
import junit.framework.Assert;
import security.model.ArmingStatus;
import security.model.SecurityRepository;
import security.model.Sensor;
import security.model.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import security.service.SecurityService;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static security.model.AlarmStatus.*;
import static security.model.ArmingStatus.*;
import static org.mockito.Mockito.*;

/**
 * @author DMalonas
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    public static final int WANTED_NUMBER_OF_INVOCATIONS = 1;

    @Mock
    ImageService imageService;

    @Mock
    SecurityRepository securityRepository;

    private SecurityService securityService;
    private Sensor sensor;


    @BeforeEach
    public void setup() {
        securityService = new SecurityService(securityRepository, imageService);
    }
    //1    If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @Test
    public void armedAlarm_activatedSensor_putSystemInPendingAlarmStatus() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(PENDING_ALARM);

    }

    //2    If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @Test
    void armedAlarm_activatedSensor_systemPending_setAlarmToAlarm() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Assert.assertEquals(PENDING_ALARM, securityRepository.getAlarmStatus());
        Assert.assertEquals(ARMED_HOME, securityRepository.getArmingStatus());
    }


    //3    If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void pendingAlarm_inactiveSensor_setAlarmToNoAlarm() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        Assert.assertEquals(PENDING_ALARM, securityRepository.getAlarmStatus());
    }

    //4 If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    void alarmActive_changeInSensorState_alarmStateStaysSame() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getAlarmStatus()).thenReturn(ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        Assert.assertEquals(ALARM, securityRepository.getAlarmStatus());
    }

    //5    If a sensor is activated while already active
    //     and the system is in pending state,
    //     change it to alarm state.
    @Test
    void alarmPending_ActiveToActiveSensorState_alarmStateGoesToAlarm() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        Assert.assertEquals(PENDING_ALARM, securityRepository.getAlarmStatus());
    }

    // 6   If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    void alarmAny_InactiveToInActiveSensorState_alarmStateStaysSame() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);
        Assert.assertEquals(PENDING_ALARM, securityRepository.getAlarmStatus());
    }

    //7    If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    void imageService_spotsImageWithCat_HomeIsArmed_putToAlarmStatus() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(Boolean.TRUE);
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        securityService.processImage(new BufferedImage(255, 255, BufferedImage.TYPE_INT_RGB));
        verify(securityRepository, times(WANTED_NUMBER_OF_INVOCATIONS)).setAlarmStatus(ALARM);
    }

    // 8   If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void imageService_spotsImageWithoutCat_homeIsArmed_sensorsInactive_putToAlarmStatus() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(Boolean.TRUE);
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        securityService.processImage(new BufferedImage(255, 255, BufferedImage.TYPE_INT_RGB));
        verify(securityRepository, times(WANTED_NUMBER_OF_INVOCATIONS)).setAlarmStatus(ALARM);
    }


    @Test
    void hanldeSensorActivatedAlarmStatus() {
        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(ALARM);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository, times(1)).setAlarmStatus(NO_ALARM);
    }
    //9    If the system is disarmed, set the status to no alarm.

    @Test
    void hanldeSensorDeActivatedAlarmStatus() {
        sensor = new Sensor("sensor", SensorType.WINDOW);
        sensor.setActive(true);
        Set<Sensor> sensorsSet = new HashSet<>(Arrays.asList(sensor));
        when(securityRepository.getSensors()).thenReturn(sensorsSet);

        when(securityRepository.getAlarmStatus()).thenReturn(ALARM);
        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);
        Assert.assertTrue(securityRepository.getAlarmStatus() == ALARM);
        verify(securityRepository, times( 0)).setAlarmStatus(ALARM);

        sensor.setActive(true);
        sensorsSet = new HashSet<>(Arrays.asList(sensor));
        when(securityRepository.getSensors()).thenReturn(sensorsSet);
        when(securityRepository.getAlarmStatus()).thenReturn(PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);
        verify(securityRepository, times( 1)).setAlarmStatus(NO_ALARM);

    }
    @Test
    void systemDisarmed_setStatusToNoAlarm() {

        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(DISARMED);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository, times(WANTED_NUMBER_OF_INVOCATIONS - 1)).setAlarmStatus(ALARM);
    }

    //10    If the system is armed, reset all sensors to inactive.
    @Test
    void systemArmed_resetSensors() {
        sensor = new Sensor("sensor", SensorType.WINDOW);
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn((Set.of(sensor)));
        securityService.setArmingStatus(ARMED_HOME);
        Assert.assertTrue(!sensor.getActive());
        sensor.setActive(true);
        securityService.setArmingStatus(ARMED_AWAY);
        Assert.assertTrue(!sensor.getActive());
    }


    @Test
    void setArmingStatusTest() {
        sensor = new Sensor("sensor", SensorType.WINDOW);
        when(securityRepository.getAlarmStatus()).thenReturn(NO_ALARM);
        securityService.setArmingStatus(DISARMED);
        Assert.assertTrue(securityService.getAlarmStatus() == NO_ALARM);
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn((Set.of(sensor)));
        securityService.setCatWasDetected(false);
        securityService.setArmingStatus(ARMED_AWAY);
        Assert.assertTrue(!sensor.getActive());

        sensor.setActive(true);
        securityService.setCatWasDetected(false);
        securityService.setArmingStatus(ARMED_HOME);
        Assert.assertTrue(!sensor.getActive());

        securityService.setCatWasDetected(true);
        securityService.setArmingStatus(ARMED_HOME);
    }
    //11    If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void systemArmedHome_cameraSpotsCat_setStatusToAlarm() {

        when(securityRepository.getArmingStatus()).thenReturn(ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(Boolean.TRUE);
        securityService.processImage(new BufferedImage(255, 255, BufferedImage.TYPE_INT_RGB));
        verify(securityRepository, times(WANTED_NUMBER_OF_INVOCATIONS)).setAlarmStatus(ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void system_armed_resetSensors(ArmingStatus armingStatus) {
        sensor = new Sensor("sensor", SensorType.WINDOW);
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn((Set.of(sensor)));
        securityService.setArmingStatus(armingStatus);
        Assert.assertTrue(!sensor.getActive());
    }
}
