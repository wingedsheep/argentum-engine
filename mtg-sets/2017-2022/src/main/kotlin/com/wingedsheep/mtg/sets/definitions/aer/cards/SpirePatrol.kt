package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spire Patrol
 * {2}{W}{U}
 * Creature — Human Soldier
 * 3/2
 * Flying
 * When this creature enters, tap target creature an opponent controls. That creature doesn't untap
 * during its controller's next untap step.
 */
val SpirePatrol = card("Spire Patrol") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Flying\nWhen this creature enters, tap target creature an opponent controls. That " +
        "creature doesn't untap during its controller's next untap step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Dan Murayama Scott"
        flavorText = "\"After I examine your cargo and check your papers, you can be on your way.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1daaf494-70af-4a8d-836f-f6a9d6c1f080.jpg"
        ruling("2017-02-09", "Spire Patrol’s ability can target a creature that’s already tapped. That creature won’t untap during its controller’s next untap step.")
    }
}
