package bep.hax.modules;

import bep.hax.Bep;
import bep.hax.BlackOutModule;

/**
 * @author OLEPOSSU
 */

public class AntiCrawl extends BlackOutModule {
    public AntiCrawl() {
        super(Bep.BLACKOUT, "Anti Crawl", "Doesn't crawl or sneak when in low space (should be used on 1.12.2).");
    }
}
