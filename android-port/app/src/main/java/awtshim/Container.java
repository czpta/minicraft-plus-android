package awtshim;
public class Container extends Component {
	/** top = vertical shift of the game view in px (negative moves it up; used while the soft keyboard is open). */
	public Insets getInsets() {
		Insets i = new Insets();
		i.top = minicraft.core.AndroidBridge.yShift;
		return i;
	}
}
