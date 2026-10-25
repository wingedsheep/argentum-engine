package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fifty Feet of Rope
 * {1}
 * Artifact
 * Climb Over — {T}: Target Wall can't block this turn.
 * Tie Up — {3}, {T}: Target creature doesn't untap during its controller's next untap step.
 * Rappel Down — {4}, {T}: Venture into the dungeon. Activate only as a sorcery.
 *
 * The ability names are flavor words with no rules meaning. Tie Up is Barl's Cage's
 * `DOESNT_UNTAP` grant, bounded by the *affected* creature's controller's next untap step.
 */
val FiftyFeetOfRope = card("Fifty Feet of Rope") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Climb Over — {T}: Target Wall can't block this turn.\n" +
        "Tie Up — {3}, {T}: Target creature doesn't untap during its controller's next untap step.\n" +
        "Rappel Down — {4}, {T}: Venture into the dungeon. Activate only as a sorcery. (Enter the first room or advance to the next room.)"

    activatedAbility {
        cost = AbilityCost.Tap
        val wall = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Wall")))
        effect = Effects.CantBlock(wall)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.VentureIntoTheDungeon()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba7ebe7b-732f-4245-a194-e087c3878f61.jpg?1783926439"
        ruling("2021-07-23", "Activating the Climb Over ability after a Wall has blocked won't change or undo that block.")
        ruling("2021-07-23", "The Tie Up ability doesn't tap the creature, it just prevents it from untapping if it is already tapped or becomes tapped through some other means.")
        ruling("2021-07-23", "Choosing the dungeon or room to venture into is part of resolving the venture into the dungeon keyword action. Once that choice is made, players may not respond until after the appropriate room ability has triggered.")
    }
}
