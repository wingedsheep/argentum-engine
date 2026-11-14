package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val LILIANA_EMBLEM =
    "Whenever a creature dies, return it to the battlefield under your control at the beginning of the next end step."

/**
 * Liliana, Heretical Healer // Liliana, Defiant Necromancer — Magic Origins #106
 * {1}{B}{B} · Legendary Creature — Human Cleric 2/3 // Legendary Planeswalker — Liliana (loyalty 3)
 *
 * Front:
 *   Lifelink
 *   Whenever another nontoken creature you control dies, exile Liliana, then return her to the
 *   battlefield transformed under her owner's control. If you do, create a 2/2 black Zombie
 *   creature token.
 * Back:
 *   +2: Each player discards a card.
 *   −X: Return target nonlegendary creature card with mana value X from your graveyard to the
 *       battlefield.
 *   −8: You get an emblem with "Whenever a creature dies, return it to the battlefield under your
 *       control at the beginning of the next end step."
 *
 * Modeling notes:
 *  - "If you do" reads whether *this* resolution brought Liliana back as the planeswalker: after
 *    [Effects.ExileAndReturnTransformed], `Self` follows the moves this resolution made, so
 *    gathering it and keeping it only if it is a planeswalker is non-empty exactly when the
 *    exile-and-return happened. When several nontoken creatures die at once, the first trigger to
 *    resolve flips her; the rest find their source a new object (CR 400.7), exile nothing, and make
 *    no Zombie (ruling 2015-06-22). The same identity check means a Liliana that died alongside
 *    the creature stays in the graveyard.
 *  - +2 is [Effects.EachPlayerDiscards]: you choose first, then each other player in turn order,
 *    and only then are all the chosen cards discarded at once — nobody sees an earlier player's
 *    discard before choosing (ruling 2015-06-22).
 *  - −X's X is fixed as the loyalty cost is paid, so `manaValueEqualsX()` constrains the target.
 *  - The emblem's "it" is the card that died: a delayed trigger moves it from the graveyard at the
 *    next end step, so a token (which ceased to exist) or a card that has left the graveyard
 *    returns nothing.
 */
private val LilianaHereticalHealerFront = card("Liliana, Heretical Healer") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "Lifelink\n" +
        "Whenever another nontoken creature you control dies, exile Liliana, Heretical Healer, then " +
        "return her to the battlefield transformed under her owner's control. If you do, create a 2/2 " +
        "black Zombie creature token."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken().youControl()).dies()
        effect = Effects.Pipeline {
            run(Effects.ExileAndReturnTransformed(EffectTarget.Self))
            val liliana = gather(CardSource.Entity(EffectTarget.Self))
            ifNotEmpty(liliana, GameObjectFilter.Planeswalker) {
                run(
                    Effects.CreateToken(
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.BLACK),
                        creatureTypes = setOf("Zombie"),
                        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5ccae95-95c2-4d11-aa68-5c80ecf90fd2.jpg?1783938294",
                    )
                )
            }
        }
        description = "Whenever another nontoken creature you control dies, exile Liliana, then return her " +
            "to the battlefield transformed under her owner's control. If you do, create a 2/2 black " +
            "Zombie creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Karla Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f25e1cf-eeb4-458d-8fb2-b3a2f86bdd54.jpg?1783938343"

        ruling("2017-09-29", "You can control two of this permanent, one front-face up and the other back-face up, at the same time.")
        ruling("2015-06-22", "If multiple nontoken creatures you control die, then Liliana, Heretical Healer's ability will trigger that many times. However, since Liliana can be exiled and returned to the battlefield only once, only the first ability to resolve will create a Zombie token. The other abilities will resolve but won't do anything.")
        ruling("2015-06-22", "A Magic Origins planeswalker that enters the battlefield because of the ability of its front face will enter with loyalty counters as normal.")
        ruling("2015-06-22", "In some rare cases, a spell or ability may cause one of these five cards to transform while it's a creature (front face up) on the battlefield. If this happens, the resulting planeswalker won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
        ruling("2015-06-22", "You can activate one of the planeswalker's loyalty abilities the turn it enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if the planeswalker enters the battlefield during combat, there will be an opportunity for your opponent to remove it before you can activate one of its abilities.")
    }
}

private val LilianaDefiantNecromancer = card("Liliana, Defiant Necromancer") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 3
    oracleText = "+2: Each player discards a card.\n" +
        "−X: Return target nonlegendary creature card with mana value X from your graveyard to the " +
        "battlefield.\n" +
        "−8: You get an emblem with \"$LILIANA_EMBLEM\""

    loyaltyAbility(+2) {
        effect = Effects.EachPlayerDiscards(1)
        description = "Each player discards a card."
    }

    loyaltyAbilityX {
        val creatureCard = target(
            TargetFilter(
                GameObjectFilter.Creature.nonlegendary().manaValueEqualsX().ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "Return target nonlegendary creature card with mana value X from your graveyard to the battlefield."
    }

    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Creature).dies()
                effect = Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.Move(
                        EffectTarget.TriggeringEntity,
                        Zone.BATTLEFIELD,
                        controllerOverride = EffectTarget.Controller,
                        fromZone = Zone.GRAVEYARD,
                    ),
                )
                description = LILIANA_EMBLEM
            },
            emblem = true,
            descriptionOverride = LILIANA_EMBLEM,
        )
        description = "You get an emblem with \"$LILIANA_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Karla Ortiz"
        imageUri = "https://cards.scryfall.io/normal/back/9/f/9f25e1cf-eeb4-458d-8fb2-b3a2f86bdd54.jpg?1783938343"

        ruling("2015-06-22", "When Liliana, Defiant Necromancer's first ability resolves, first you choose a card to discard, then each other player in turn order chooses a card to discard, then all those cards are discarded simultaneously. No one sees what the other players are discarding before deciding which card to discard.")
    }
}

val LilianaHereticalHealer: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LilianaHereticalHealerFront,
    backFace = LilianaDefiantNecromancer,
)
