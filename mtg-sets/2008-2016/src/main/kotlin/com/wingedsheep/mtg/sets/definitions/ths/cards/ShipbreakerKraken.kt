package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shipbreaker Kraken — Theros #63
 * {4}{U}{U} · Creature — Kraken · 6/6
 *
 * {6}{U}{U}: Monstrosity 4.
 * When this creature becomes monstrous, tap up to four target creatures. Those creatures don't
 * untap during their controllers' untap steps for as long as you control this creature.
 *
 * Sudden Storm's per-target tap + DOESNT_UNTAP grant, with Dungeon Geists' one-way
 * [Duration.WhileYouControlSource] gate (losing control ends the lock; regaining it doesn't restore it).
 */
val ShipbreakerKraken = card("Shipbreaker Kraken") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 6
    toughness = 6
    oracleText = "{6}{U}{U}: Monstrosity 4. (If this creature isn't monstrous, put four +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, tap up to four target creatures. Those creatures don't untap during their controllers' untap steps for as long as you control this creature."

    activatedAbility {
        cost = Costs.Mana("{6}{U}{U}")
        effect = Effects.Monstrosity(4)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        targets(TargetFilter.Creature, count = 4, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.WhileYouControlSource("Shipbreaker Kraken"),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4585565b-c743-4264-a647-4480ff5a3bbf.jpg?1783939790"
        ruling("2013-09-15", "If you lose control of Shipbreaker Kraken between when its last ability triggers and when that ability resolves, the target creatures will become tapped. However, they won't be affected by the other effect and will untap as normal during their controllers' untap steps.")
        ruling("2013-09-15", "If another player gains control of Shipbreaker Kraken, the creatures will no longer be affected by the effect preventing them from untapping, even if you later regain control of Shipbreaker Kraken.")
        ruling("2013-09-15", "If you gain control of a creature that's being prevented from untapping, that creature won't untap during your untap step for as long as you control Shipbreaker Kraken.")
        ruling("2013-09-15", "The ability can target a tapped creature. If a targeted creature is already tapped when it resolves, that creature just remains tapped and doesn't untap during its controller's untap step.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
