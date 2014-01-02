package jmri.jmrit.operations.trains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import jmri.jmrit.display.LocoIcon;
import jmri.jmrit.display.Editor;
import jmri.jmrit.operations.rollingstock.RollingStock;
import jmri.jmrit.operations.rollingstock.cars.Car;
import jmri.jmrit.operations.rollingstock.cars.CarManager;
import jmri.jmrit.operations.routes.Route;
import jmri.jmrit.operations.routes.RouteLocation;

/**
 * An icon that displays the position of a train icon on a panel.<P>
 * The icon can always be repositioned and its popup menu is
 * always active.
 * @author Bob Jacobsen  Copyright (c) 2002
 * @author Daniel Boudreau Copyright (C) 2008
 * @version $Revision$
 */

public class TrainIcon extends LocoIcon {

	public TrainIcon(Editor editor) {
        // super ctor call to make sure this is an icon label
    	super(editor); 
    }
	
	// train icon tool tips are always enabled
	public void setShowTooltip(boolean set){_showTooltip = true;}
 
    /**
     * Pop-up only if right click and not dragged
     * return true if a popup item is set 
     */
	public boolean showPopUp(JPopupMenu popup) {
		if (_train != null){
			popup.add(new AbstractAction(Bundle.getMessage("Move")) {
				public void actionPerformed(ActionEvent e) {
					_train.move();
				}
			});
			popup.add(makeTrainRouteMenu()); 
			popup.add(new TrainConductorAction(Bundle.getMessage("TitleTrainConductor"), _train));
			popup.add(new ShowCarsInTrainAction(Bundle.getMessage("MenuItemShowCarsInTrain"), _train));
            if (!isEditable()) {
                popup.add(new AbstractAction(Bundle.getMessage("SetX&Y")) {
                    public void actionPerformed(ActionEvent e) {
                        if(!_train.setTrainIconCoordinates())
                            JOptionPane.showMessageDialog(null, Bundle.getMessage("SeeOperationsSettings"),
                            		Bundle.getMessage("SetX&YisDisabled"),
                                    JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
		}
		popup.add(new ThrottleAction(Bundle.getMessage("Throttle"))); 
		popup.add(makeLocoIconMenu());
        if (!isEditable()) {
            getEditor().setRemoveMenu(this, popup);
        }
        return true;
	}

	Train _train = null;

	public void setTrain (Train train){
		_train = train;
	}

	public Train getTrain (){
		return _train;
	}

	int _consistNumber = 0;

	public void setConsistNumber (int cN){
		this._consistNumber =   cN;
	}

	private int getConsistNumber(){
		return _consistNumber;
	}

	jmri.jmrit.throttle.ThrottleFrame _tf = null;

	private void createThrottle(){
		_tf = jmri.jmrit.throttle.ThrottleFrameManager.instance().createThrottleFrame();
		if (getConsistNumber() > 0){
			_tf.getAddressPanel().setAddress(getConsistNumber(), false);	// use consist address
			if (JOptionPane.showConfirmDialog(null,
					Bundle.getMessage("SendFunctionCommands"), Bundle.getMessage("ConsistThrottle"),
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				_tf.getAddressPanel().setRosterEntry(_entry);			 	// use lead loco address
			}			
		} else {
			_tf.getAddressPanel().setRosterEntry(_entry);
		}
		_tf.toFront();
	}

	private JMenu makeTrainRouteMenu(){
		JMenu routeMenu = new JMenu(Bundle.getMessage("Route"));
		Route route = _train.getRoute();
		if (route == null)
			return routeMenu;
		List<String> routeList = route.getLocationsBySequenceList();
		CarManager carManager = CarManager.instance();
		List<RollingStock> carList = carManager.getByTrainList(_train);
		for (int r=0; r<routeList.size(); r++){
			int pickupCars = 0;
			int dropCars = 0;
			String current = "     ";
			RouteLocation rl = route.getLocationById(routeList.get(r));
			if (_train.getCurrentLocation() == rl)
				current = "-> "; // NOI18N
			for (int j=0; j<carList.size(); j++){
				Car car = (Car) carList.get(j);
				if (car.getRouteLocation() == rl && !car.getTrackName().equals("")){
					pickupCars++;
				}
				if (car.getRouteDestination() == rl){
					dropCars++;
				}
			}
			String rText = "";
			String pickups = "";
			String drops = "";
			if (pickupCars > 0){
				pickups = " "+Bundle.getMessage("Pickup")+" " + pickupCars;
				if (dropCars > 0)
					drops = ", "+Bundle.getMessage("SetOut")+" " + dropCars;
			}
			else if (dropCars > 0)
				drops = " "+Bundle.getMessage("SetOut")+" " + dropCars;
			if (pickupCars > 0 || dropCars > 0)
				rText = current + rl.getName() +"  (" + pickups + drops +" )";
			else
				rText = current + rl.getName();
			routeMenu.add(new RouteAction(rText, rl));
		}
		return routeMenu;
	}

	public class ThrottleAction extends AbstractAction {
	    public ThrottleAction(String actionName) {
	        super(actionName);
			if (_entry == null)
				setEnabled(false);
	    }
		public void actionPerformed(ActionEvent e) {
			createThrottle();
		}
	}
	
	/**
	 * Moves train from current location to the one selected by user.
	 *
	 */
	public class RouteAction extends AbstractAction{
		RouteLocation _rl;
		public RouteAction(String actionName, RouteLocation rl) {
			super(actionName);
			_rl = rl;
		}
		public void actionPerformed(ActionEvent e) {
			log.debug("Route location selected "+_rl.getName());
			Route route = _train.getRoute();
			List<String> routeList = route.getLocationsBySequenceList();
			// determine where the train is in the route
			for (int r=0; r<routeList.size(); r++){
				RouteLocation rl = route.getLocationById(routeList.get(r));
				if (_train.getCurrentLocation() == rl){
					log.debug("Train is at location "+rl.getName());
					// Is train at this route location?
					if (rl == _rl)
						break;
					for (int i=r+1; i<routeList.size(); i++){
						RouteLocation nextRl = route.getLocationById(routeList.get(i));
						// did user select the next location in the route?
						if (nextRl == _rl && i == r+1){
							_train.move();
						}else if (nextRl == _rl){
							if (JOptionPane.showConfirmDialog(null,
									MessageFormat.format(Bundle.getMessage("MoveTrainTo"), new Object [] { _rl.getName() }),
									MessageFormat.format(Bundle.getMessage("MoveTrain"), new Object [] { _train.getIconName() }),
									JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
								while (_train.getCurrentLocation() != _rl){
									_train.move();
								}
							}
						}
					}
				}
			}
		}
	}
	
	private final int range = 25; // how close the train icon needs to be to the next location coordinates in a train's route
	
	/**
	 * Determine if user moved the train icon to next location in a train's route.
	 */
	public void doMouseDragged(MouseEvent event) {
		log.debug("Mouse dragged, X=" + getX() + " Y=" + getY());
		if (_train != null) {
			RouteLocation next = _train.getNextLocation(_train.getCurrentLocation());
			Point nextPoint = null;
			if (next != null && ((nextPoint = next.getTrainIconCoordinates()) != null)) {
				log.debug("Next location (" + next.getName() + "), X=" + nextPoint.x + " Y=" + nextPoint.y);
				if (Math.abs(getX() - nextPoint.x) < range && Math.abs(getY() - nextPoint.y) < range) {
					log.debug("Train icon ("+_train.getName()+") within range of (" + next.getName() + ")");
					if (JOptionPane.showConfirmDialog(null, MessageFormat.format(Bundle.getMessage("MoveTrainTo"),
							new Object[] { next.getName() }), MessageFormat.format(Bundle.getMessage("MoveTrain"),
							new Object[] { _train.getIconName() }), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						_train.move();
					}
				}
			}
		}
	}
	
	static Logger log = LoggerFactory.getLogger(TrainIcon.class.getName());
}
