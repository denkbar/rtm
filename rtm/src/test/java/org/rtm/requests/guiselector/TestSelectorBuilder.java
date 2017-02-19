package org.rtm.requests.guiselector;

import java.util.ArrayList;
import java.util.List;

public class TestSelectorBuilder {

	public static List<Selector> buildSimpleSelectorList() {
		
		List<Selector> selList = new ArrayList<>();
		
		NumericalFilter nf = new NumericalFilter();
		nf.setKey("value");
		nf.setMinValue(0L);
		nf.setMaxValue(10000L);

		TextFilter tf = new TextFilter();
		tf.setKey("name");
		tf.setValue("Transaction");

		TextFilter regTf = new TextFilter();
		regTf.setKey("eId");
		regTf.setValue("JUnit.*");
		regTf.setRegex(true);

		Selector sel = new Selector();
		sel.addNumericalFilter(nf);
		sel.addTextFilter(tf);
		sel.addTextFilter(regTf);

		selList.add(sel);

		return selList;
	}

}