package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Amulet of Safekeeping — Core Set 2019 #226 (canonical printing)
 * Artifact
 *
 * The trigger is the player-only becomes-target wording: only *you* being targeted fires it, never a
 * permanent you control (contrast Valkmira's "you or another permanent you control"). "Creature tokens"
 * is every player's tokens.
 */
val AmuletOfSafekeeping = card("Amulet of Safekeeping") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you become the target of a spell or ability an opponent controls, counter that " +
        "spell or ability unless its controller pays {1}.\n" +
        "Creature tokens get -1/-0."

    triggeredAbility {
        trigger = Triggers.you.becomesTarget(byOpponent = true)
        effect = Effects.CounterTargetingSourceUnlessPays(WardCost.Mana("{1}"))
        description = "Whenever you become the target of a spell or ability an opponent controls, counter " +
            "that spell or ability unless its controller pays {1}."
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = -1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.token())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Aaron Miller"
        flavorText = "Whether it is magical, lucky, or merely old, it is cherished, and that gives it power."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d45ac8dc-281f-4257-9658-80af65a0295b.jpg?1783934517"
    }
}
