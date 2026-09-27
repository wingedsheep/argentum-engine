package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Iname, Life Aspect
 * {4}{G}{G}
 * Legendary Creature — Spirit
 * 4/4
 * When Iname dies, you may exile it. If you do, return any number of target Spirit cards from
 * your graveyard to your hand.
 *
 * The targets are chosen as the trigger goes on the stack (one `unlimited = true` slot, so zero
 * is legal); the exile is a resolution-time "you may". The dies trigger functions from the
 * graveyard so "exile it" can reference Self, and `IfYouDo`'s zone-move criterion makes the
 * return depend on the exile actually happening — if Iname already left the graveyard, nothing
 * is returned. "Spirit cards" is a bare tribal noun, so any card with the subtype qualifies.
 */
val InameLifeAspect = card("Iname, Life Aspect") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "When Iname dies, you may exile it. If you do, return any number of target " +
        "Spirit cards from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.dies()
        triggerZone = Zone.GRAVEYARD
        targets(
            TargetFilter.CardInGraveyard.withSubtype(Subtype.SPIRIT).ownedByYou(),
            unlimited = true
        )
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Move(EffectTarget.Self, Zone.EXILE),
                then = Effects.Pipeline {
                    val spiritCards = gather(CardSource.ChosenTargets)
                    move(spiritCards, CardDestination.ToZone(Zone.HAND, Player.You))
                }
            ),
            descriptionOverride = "You may exile Iname. If you do, return any number of target " +
                "Spirit cards from your graveyard to your hand."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "Justin Sweet"
        flavorText = "Iname rejoices in the dawn of a new life, but soon becomes jealous of the " +
            "simple joys denied him by his station. So the cycle begins anew."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be027e3c-8891-46f0-bca7-d28a94ca281a.jpg?1783944289"
        ruling(
            "2004-12-01",
            "The targets of Iname, Life Aspect's ability are chosen when the ability is put onto " +
                "the stack, but you don't choose whether to exile Iname, Life Aspect until the " +
                "ability resolves."
        )
        ruling(
            "2004-12-01",
            "You may target Iname, Life Aspect. However, this doesn't do anything since the card " +
                "will no longer be in your graveyard when the creature cards are returned to your hand."
        )
        ruling("2004-12-01", "You can exile the card even if you choose no targets.")
    }
}
