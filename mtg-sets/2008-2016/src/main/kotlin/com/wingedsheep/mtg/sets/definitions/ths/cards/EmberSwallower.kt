package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ember Swallower — Theros #120
 * {2}{R}{R} · Creature — Elemental · 4/5
 *
 * {5}{R}{R}: Monstrosity 3.
 * When this creature becomes monstrous, each player sacrifices three lands of their choice.
 *
 * Each player choosing three lands, then all being sacrificed at once, is [Effects.Sacrifice]
 * aimed at [Player.Each] — every player's choice lands in one simultaneous sacrifice event.
 */
val EmberSwallower = card("Ember Swallower") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 5
    oracleText = "{5}{R}{R}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, each player sacrifices three lands of their choice."

    activatedAbility {
        cost = Costs.Mana("{5}{R}{R}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        effect = Effects.Sacrifice(
            GameObjectFilter.Land,
            count = 3,
            target = EffectTarget.PlayerRef(Player.Each),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2715851-9def-42a0-bed4-0923e599e19a.jpg?1783939763"
        ruling("2013-09-15", "When the last ability resolves, the active player will choose three lands they control, then each other player in turn order will do the same. Then all of those lands are sacrificed at the same time.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
