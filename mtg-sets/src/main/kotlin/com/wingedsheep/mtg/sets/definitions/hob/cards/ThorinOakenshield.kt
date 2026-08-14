package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thorin Oakenshield
 * {R}{W}
 * Legendary Creature — Dwarf Noble
 * 3/2
 *
 * Trample
 * Storied.
 * As long as you have an enduring story, artifacts and creatures you control have ward {1}.
 *
 * "Artifacts and creatures you control" is one homogeneous union — both branches share the
 * you-control gate and differ only in card type — so `Artifact.youControl() or Creature.youControl()`
 * collapses to a flat `CardPredicate.Or`, which is the representation the lord/projection machinery
 * already understands. An artifact creature is one permanent in that set and gets one instance of
 * ward, not two.
 *
 * Ward itself is granted through [GrantWard] rather than a bare [Keyword.WARD] grant because ward is
 * parameterized: the static projects the keyword for display while the engine's
 * `TriggerAbilityResolver.getWardTriggeredAbilities` generates the actual "whenever this becomes the
 * target of a spell or ability an opponent controls, counter it unless they pay {1}" trigger from the
 * [WardCost]. Wrapping it in a [ConditionalStaticAbility] keeps that in the layer system, so the ward
 * appears and disappears with the enduring story — which, since the designation is never lost once
 * gained, means it appears once and stays.
 */
val ThorinOakenshield = card("Thorin Oakenshield") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Noble"
    oracleText = "Trample\n" +
        "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you have an " +
        "enduring story for the rest of the game.)\n" +
        "As long as you have an enduring story, artifacts and creatures you control have ward {1}."
    power = 3
    toughness = 2

    keywords(Keyword.TRAMPLE)
    storied()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantWard(
                cost = WardCost.Mana("{1}"),
                filter = GroupFilter(
                    GameObjectFilter.Artifact.youControl() or GameObjectFilter.Creature.youControl()
                )
            ),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "165"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7e18609-d1ed-4829-be11-f2ce2cfcbc49.jpg?1784377040"
    }
}
