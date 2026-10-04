package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grist, Voracious Larva // Grist, the Plague Swarm — Modern Horizons 3 #251
 * {G} · Legendary Creature — Insect 1/2 // Legendary Planeswalker — Grist (loyalty 3)
 *
 * Modeling notes:
 *  - Front: "Grist or another creature you control enters" is [Triggers.a] over creatures you
 *    control (the ANY binding includes Grist itself). "If it entered from your graveyard or you cast
 *    it from your graveyard" is the intervening-if
 *    [Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard] (CR 603.4), which only knows "a
 *    graveyard"; a card is only ever in its owner's graveyard, so the trigger filter also requires
 *    that you own the creature (a creature reanimated from an opponent's graveyard doesn't count).
 *    The flip is
 *    [Effects.MayPay] `{G}` into [Effects.ExileAndReturnTransformed]; if Grist has left the
 *    battlefield by then, nothing is exiled or returned (ruling).
 *  - +1: the token is published as `CREATED_TOKENS`; the mill is its own collection, and a black
 *    card among the milled cards puts a deathtouch counter on the token.
 *  - −6: every creature card in your graveyard is gathered at resolution and each gets a token copy
 *    with the printed exceptions — 1/1, black and green, Insect instead of its other creature types.
 */
private const val INSECT_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/a/5/a5bc4cb0-60d4-4c8f-a420-8bfe635154cd.jpg?1783911112"

private val GristVoraciousLarvaFront = card("Grist, Voracious Larva") {
    manaCost = "{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Insect"
    power = 1
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever Grist or another creature you control enters, if it entered from your graveyard " +
        "or you cast it from your graveyard, you may pay {G}. If you do, exile Grist, then return " +
        "it to the battlefield transformed under its owner's control."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withControllerPredicate(
                ControllerPredicate.And(listOf(ControllerPredicate.ControlledByYou, ControllerPredicate.OwnedByYou))
            )
        ).enters()
        interveningIf = Conditions.TriggeringEntityEnteredOrWasCastFromGraveyard
        effect = Effects.MayPay(
            cost = ManaCost.parse("{G}"),
            then = Effects.ExileAndReturnTransformed(EffectTarget.Self),
        )
        description = "Whenever Grist or another creature you control enters, if it entered from " +
            "your graveyard or you cast it from your graveyard, you may pay {G}. If you do, exile " +
            "Grist, then return it to the battlefield transformed under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "251"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68239b41-b7db-4044-b672-6808c2c342ec.jpg?1783911227"

        ruling("2024-06-07", "In some rare cases, a spell or ability may cause Grist, Voracious Larva to transform while it's a creature (front face up) on the battlefield. If this happens, Grist, the Plague Swarm won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
        ruling("2024-06-07", "If Grist, Voracious Larva leaves the battlefield while its triggered ability is on the stack, you can't exile it from the zone it's put into, so you won't return it to the battlefield transformed under its owner's control.")
    }
}

private val GristThePlagueSwarm = card("Grist, the Plague Swarm") {
    manaCost = ""
    colorIdentity = "BG"
    colorIndicator = "BG"
    typeLine = "Legendary Planeswalker — Grist"
    startingLoyalty = 3
    oracleText = "+1: Create a 1/1 black and green Insect creature token, then mill two cards. Put " +
        "a deathtouch counter on the token if a black card was milled this way.\n" +
        "−2: Destroy target artifact or enchantment.\n" +
        "−6: For each creature card in your graveyard, create a token that's a copy of it, except " +
        "it's a 1/1 black and green Insect."

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            run(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLACK, Color.GREEN),
                    creatureTypes = setOf("Insect"),
                    imageUri = INSECT_TOKEN_IMAGE,
                )
            )
            val milled = mill(2)
            run(
                Effects.If(
                    condition = whenMatches(milled, GameObjectFilter.Any.withColor(Color.BLACK)),
                    then = Effects.AddCountersToCollection(CollectionSlot.CreatedTokens, CounterType.DEATHTOUCH, 1),
                )
            )
        }
        description = "Create a 1/1 black and green Insect creature token, then mill two cards. Put " +
            "a deathtouch counter on the token if a black card was milled this way."
    }

    loyaltyAbility(-2) {
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(permanent)
        description = "Destroy target artifact or enchantment."
    }

    loyaltyAbility(-6) {
        effect = Effects.Pipeline {
            val creatureCards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
            run(
                Effects.ForEachInCollection(
                    collection = creatureCards,
                    effect = Effects.CreateTokenCopyOfTarget(
                        target = EffectTarget.IterationEntity,
                        overridePower = 1,
                        overrideToughness = 1,
                        overrideColors = setOf(Color.BLACK, Color.GREEN),
                        overrideSubtypes = setOf(Subtype.INSECT),
                    ),
                )
            )
        }
        description = "For each creature card in your graveyard, create a token that's a copy of " +
            "it, except it's a 1/1 black and green Insect."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "251"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/back/6/8/68239b41-b7db-4044-b672-6808c2c342ec.jpg?1783911227"

        ruling("2024-06-07", "You can activate one of Grist, the Plague Swarm's loyalty abilities the turn it enters the battlefield. However, you may do so only during one of your main phases when the stack is empty.")
        ruling("2024-06-07", "Except for the listed exceptions, the tokens created by Grist, the Plague Swarm's last ability copy exactly what was printed on the original cards and nothing else. They don't copy any information about the objects those cards were before they were put into your graveyard.")
        ruling("2024-06-07", "The tokens are Insects instead of their other creature types, and they're black and green instead of their other colors. These are copiable values of the tokens that other effects may copy.")
        ruling("2024-06-07", "If any of the copied cards have {X} in their mana costs, {X} is 0.")
    }
}

val GristVoraciousLarva: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = GristVoraciousLarvaFront,
    backFace = GristThePlagueSwarm,
)
