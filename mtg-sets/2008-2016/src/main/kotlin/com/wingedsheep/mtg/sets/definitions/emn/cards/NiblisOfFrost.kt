package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Niblis of Frost (Eldritch Moon #72)
 * {2}{U}{U}
 * Creature — Spirit
 * 3/3
 *
 * Flying
 * Prowess
 * Whenever you cast an instant or sorcery spell, tap target creature an opponent controls. That
 * creature doesn't untap during its controller's next untap step.
 */
val NiblisOfFrost = card("Niblis of Frost") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Whenever you cast an instant or sorcery spell, tap target creature an opponent controls. " +
        "That creature doesn't untap during its controller's next untap step."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)
    prowess()

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap(),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "72"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da2301c8-0ca8-4b26-a232-1df90effcb42.jpg?1783937500"
        ruling(
            "2016-07-13",
            "If multiple effects say that a creature doesn't untap during its controller's next untap " +
                "step, those effects all apply during one untap step. For example, if you cast two " +
                "instants in one turn and target the same creature twice with Niblis of Frost's " +
                "ability, that creature will only spend one untap step without untapping."
        )
    }
}
