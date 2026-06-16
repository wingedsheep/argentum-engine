package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Furious Forebear
 * {1}{W}
 * Creature — Spirit Warrior
 * 3/1
 *
 * Whenever a creature you control dies while this card is in your graveyard, you may pay {1}{W}.
 * If you do, return this card from your graveyard to your hand.
 *
 * Modeled as a graveyard-zone triggered ability (triggerZone = GRAVEYARD), gated to fire only
 * while Furious Forebear sits in its owner's graveyard. The "you may pay {1}{W}" clause uses
 * [MayPayManaEffect]; on payment the card returns itself from the graveyard to hand.
 *
 * The engine evaluates graveyard triggers after the death move, so this uses OTHER binding to
 * preserve the printed timing: Forebear must already be in the graveyard before the creature dies.
 */
val FuriousForebear = card("Furious Forebear") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit Warrior"
    power = 3
    toughness = 1
    oracleText = "Whenever a creature you control dies while this card is in your graveyard, " +
        "you may pay {1}{W}. If you do, return this card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER,
        )
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}{W}"),
            effect = Effects.ReturnToHand(EffectTarget.Self)
        )
        description = "Whenever a creature you control dies while this card is in your graveyard, " +
            "you may pay {1}{W}. If you do, return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Izzy"
        flavorText = "\"Your hatred dampens the land with my kin's blood. My love will flood it with yours.\"\n—Hazena, spirit of House Emesh"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4f247b6-8212-4e78-a452-d2d3be228d8e.jpg?1743204001"
    }
}
