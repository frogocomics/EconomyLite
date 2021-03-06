package me.Flibio.EconomyLite.API;

import me.Flibio.EconomyLite.EconomyLite;

import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LiteCurrency implements Currency {
	
	public LiteCurrency() {
		
	}

	@Override
	public int getDefaultFractionDigits() {
		return 0;
	}

	@Override
	public Text getDisplayName() {
		return Text.of(EconomyLite.access.currencySingular);
	}

	@Override
	public Text getPluralDisplayName() {
		return Text.of(EconomyLite.access.currencyPlural);
	}

	@Override
	public Text getSymbol() {
		return Text.of(EconomyLite.access.currencyPlural.substring(0, 1));
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public Text format(BigDecimal amount, int arg1) {
		int rounded = amount.setScale(0, RoundingMode.HALF_UP).intValue();
		if(rounded==1) { 
			return Text.of(rounded," ",EconomyLite.access.currencySingular);
		} else {
			return Text.of(rounded," ",EconomyLite.access.currencyPlural);
		}
	}

}
