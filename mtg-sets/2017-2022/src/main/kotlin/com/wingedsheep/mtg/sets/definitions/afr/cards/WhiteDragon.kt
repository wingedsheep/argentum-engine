package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * White Dragon
 * {4}{W}{W}
 * Creature — Dragon
 * 4/4
 * Flying
 * Cold Breath — When this creature enters, tap target creature an opponent controls. That creature
 * doesn't untap during its controller's next untap step.
 *
 * Same ETB as Frost Trickster: [Effects.Tap] then a DOESNT_UNTAP flag lasting until after the
 * affected controller's next untap step.
 */
val WhiteDragon = card("White Dragon") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nCold Breath — When this creature enters, tap target creature an opponent controls. " +
        "That creature doesn't untap during its controller's next untap step."
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Billy Christian"
        flavorText = "White dragons are unparalleled hunters, keenly focused on slaughtering their enemies."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21e290c4-6c94-472a-9dec-6994aade29c2.jpg?1783926522"
    }
}
