package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val VesuvanDoppelganger = card("Vesuvan Doppelganger") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, except it doesn't copy that creature's color and it has \"At the beginning of your upkeep, you may have this creature become a copy of target creature, except it doesn't copy that creature's color and it has this ability.\""

    val upkeepCopy = grantedTriggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val creature = target(TargetFilter.Creature)
        description = "At the beginning of your upkeep, you may have this creature become a copy of target creature, except it doesn't copy that creature's color and it has this ability."
        effect = Effects.May(Effects.EachPermanentBecomesCopyOfTarget(
            target = creature,
            affected = EffectTarget.Self,
            exceptions = CopyExceptions(retainColors = true, retainResolvingTriggeredAbility = true),
        ), sourceRequiredZone = Zone.BATTLEFIELD, descriptionOverride = description)
    }
    replacementEffect(EntersAsCopy(
        optional = true,
        exceptions = CopyExceptions(retainColors = true, addedTriggeredAbilities = listOf(upkeepCopy)),
    ))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/768f3a05-bd06-4a23-b9f2-94f6e618fd9f.jpg?1783948699"
        ruling("2007-09-16", "Although Vesuvan Doppelganger's triggered ability is targeted, its \"as this creature enters\" ability is not.")
        ruling("2007-09-16", "When Vesuvan Doppelganger's triggered ability triggers, you must choose a target for it. You determine whether to have the Doppelganger become a copy of that target when the ability resolves.")
        ruling("2007-09-16", "Vesuvan Doppelganger copies the mana cost of the creature it's copying but doesn't copy its color.")
        ruling("2007-09-16", "If another creature copies Vesuvan Doppelganger, the new creature will become a copy of whatever Vesuvan Doppelganger is copying except for its color, will copy Vesuvan Doppelganger's color, and will gain Vesuvan Doppelganger's triggered ability.")
        ruling("2007-09-16", "Vesuvan Doppelganger's triggered ability may cause it to become a copy of itself. A Vesuvan Doppelganger that becomes a copy of a Vesuvan Doppelganger (either itself or a different one) will gain another instance of its triggered ability. Each instance of that ability will trigger during its controller's upkeep. They'll all be put on the stack and resolve one at a time. The last one to resolve determines the Doppelganger's characteristics for the rest of the turn.")
        ruling("2007-09-16", "Vesuvan Doppelganger copies a face-down creature, it becomes a face-up 2/2 blue creature with no name and no abilities other than the one it gives itself.")
        ruling("2004-12-01", "If a Doppelganger is flipped, and it copies a flip card in any state, it will copy both \"sides\" of that card but use the flipped side. If a Doppelganger is unflipped, and it copies a flip card in any state, it will copy both \"sides\" of that card but use the unflipped side. In the second case, if it ever meets the flip conditions of its new ability, it will flip and used the flipped side of what it is copying.")
        ruling("2004-10-04", "When the Doppelganger switches creatures, the creature it used to be is not considered to have left the battlefield. Such effects will consider the creature to have left the battlefield when the Doppelganger leaves the battlefield.")
        ruling("2004-10-04", "Damage is not removed when it changes forms.")
        ruling("2004-10-04", "Can switch to the same creature it is currently a copy of.")
        ruling("2004-10-04", "When it takes on the characteristics of the other card, it is no longer of creature type Shapeshifter.")
        ruling("2004-10-04", "When changing forms, any text changes that exist on the Doppelganger are applied to the new text, if appropriate.")
    }
}
