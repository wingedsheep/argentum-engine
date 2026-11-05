package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * March of the Drowned
 * {B}
 * Sorcery
 *
 * Choose one —
 * • Return target creature card from your graveyard to your hand.
 * • Return two target Pirate cards from your graveyard to your hand.
 *
 * The second mode needs exactly two Pirate cards (any card type carrying the Pirate subtype). Each
 * target is returned on its own, so if one leaves the graveyard in response the other still returns.
 */
val MarchOfTheDrowned = card("March of the Drowned") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Return target creature card from your graveyard to your hand.\n" +
        "• Return two target Pirate cards from your graveyard to your hand."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Return target creature card from your graveyard to your hand") {
                val creatureInYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.ReturnToHand(creatureInYourGraveyard)
            },
            Mode.withTarget(
                Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))),
                TargetObject(
                    count = 2,
                    filter = TargetFilter(
                        GameObjectFilter.Any.withSubtype(Subtype.PIRATE).ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                ),
                "Return two target Pirate cards from your graveyard to your hand"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Ben Wootten"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dfb8ae9-c6ea-4fab-ac9e-b25c62c540c6.jpg?1783935761"
        ruling("2018-01-19", "If an effect refers to a \"[subtype] spell\" or \"[subtype] card,\" it refers only to a spell or card that has that subtype. For example, March of the Drowned is a card that benefits Pirates and features Pirates in its illustration, but it isn't a Pirate card.")
    }
}
