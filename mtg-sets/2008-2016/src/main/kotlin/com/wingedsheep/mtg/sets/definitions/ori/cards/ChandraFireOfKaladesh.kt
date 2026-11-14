package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

private const val CHANDRA_EMBLEM = "At the beginning of your upkeep, this emblem deals 3 damage to you."

/**
 * Chandra, Fire of Kaladesh // Chandra, Roaring Flame — Magic Origins #135
 * {1}{R}{R} · Legendary Creature — Human Shaman 2/2 // Legendary Planeswalker — Chandra (loyalty 4)
 *
 * Front:
 *   Whenever you cast a red spell, untap Chandra.
 *   {T}: Chandra deals 1 damage to target player or planeswalker. If Chandra has dealt 3 or more
 *   damage this turn, exile her, then return her to the battlefield transformed under her owner's
 *   control.
 * Back:
 *   +1: Chandra deals 2 damage to target player or planeswalker.
 *   −2: Chandra deals 2 damage to target creature.
 *   −7: Chandra deals 6 damage to each opponent. Each player dealt damage this way gets an emblem
 *       with "At the beginning of your upkeep, this emblem deals 3 damage to you."
 *
 * Modeling notes:
 *  - The flip check reads [EntityNumericProperty.DamageDealtThisTurn] on Chandra as the ability
 *    resolves — every damage she dealt this turn, combat included (ruling 2015-06-22) — and only
 *    on resolution of the activated ability, never on its own.
 *  - −7 deals its 6 damage to every opponent as one simultaneous event, and `storeDamagedAs`
 *    collects the players actually dealt damage: an opponent whose damage was wholly prevented gets
 *    no emblem, and a player a replacement redirected the damage to does (ruling 2015-06-22). The
 *    emblem loop rebinds "you", so each recipient owns its emblem and its upkeep is theirs.
 *  - Chandra at 7 loyalty is in the graveyard when −7 resolves; she still deals the damage by
 *    last-known information.
 */
private val ChandraFireOfKaladeshFront = card("Chandra, Fire of Kaladesh") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a red spell, untap Chandra, Fire of Kaladesh.\n" +
        "{T}: Chandra, Fire of Kaladesh deals 1 damage to target player or planeswalker. If Chandra has " +
        "dealt 3 or more damage this turn, exile her, then return her to the battlefield transformed " +
        "under her owner's control."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.RED))
        effect = Effects.Untap(EffectTarget.Self)
        description = "Whenever you cast a red spell, untap Chandra, Fire of Kaladesh."
    }

    activatedAbility {
        cost = Costs.Tap
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.DamageDealtThisTurn),
                ComparisonOperator.GTE,
                3,
            ),
            Effects.ExileAndReturnTransformed(EffectTarget.Self),
        )
        description = "Chandra, Fire of Kaladesh deals 1 damage to target player or planeswalker. If Chandra " +
            "has dealt 3 or more damage this turn, exile her, then return her to the battlefield transformed " +
            "under her owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "135"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0d6caf0-4fa8-4ec5-b7f4-1307474d1b13.jpg?1783938336"

        ruling("2017-09-29", "You can control two of this permanent, one front-face up and the other back-face up, at the same time.")
        ruling("2015-06-22", "Chandra, Fire of Kaladesh's activated ability will count any damage Chandra has dealt during the turn to any permanent or player, including combat damage.")
        ruling("2015-06-22", "The last sentence of Chandra, Fire of Kaladesh's activated ability isn't a separate ability. The check happens only as that activated ability resolves. You must activate the ability in order to exile Chandra and return her to the battlefield transformed, even if Chandra has already dealt 3 or more damage during the turn.")
        ruling("2015-06-22", "A Magic Origins planeswalker that enters the battlefield because of the ability of its front face will enter with loyalty counters as normal.")
        ruling("2015-06-22", "In some rare cases, a spell or ability may cause one of these five cards to transform while it's a creature (front face up) on the battlefield. If this happens, the resulting planeswalker won't have any loyalty counters on it and will subsequently be put into its owner's graveyard.")
        ruling("2015-06-22", "You can activate one of the planeswalker's loyalty abilities the turn it enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if the planeswalker enters the battlefield during combat, there will be an opportunity for your opponent to remove it before you can activate one of its abilities.")
    }
}

private val ChandraRoaringFlame = card("Chandra, Roaring Flame") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "+1: Chandra, Roaring Flame deals 2 damage to target player or planeswalker.\n" +
        "−2: Chandra, Roaring Flame deals 2 damage to target creature.\n" +
        "−7: Chandra, Roaring Flame deals 6 damage to each opponent. Each player dealt damage this way " +
        "gets an emblem with \"$CHANDRA_EMBLEM\""

    loyaltyAbility(+1) {
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(2, victim)
        description = "Chandra, Roaring Flame deals 2 damage to target player or planeswalker."
    }

    loyaltyAbility(-2) {
        val victim = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, victim)
        description = "Chandra, Roaring Flame deals 2 damage to target creature."
    }

    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val damaged = runStoringCollection {
                Effects.DealDamage(6, EffectTarget.PlayerRef(Player.EachOpponent), storeDamagedAs = it)
            }
            run(
                Effects.ForEachPlayer(
                    damaged.asPlayers,
                    Effects.CreateGlobalTriggeredAbility(
                        ability = grantedTriggeredAbility {
                            trigger = Triggers.you.beginningOf(Step.UPKEEP)
                            effect = Effects.DealDamage(3, EffectTarget.Controller)
                            description = CHANDRA_EMBLEM
                        },
                        descriptionOverride = CHANDRA_EMBLEM,
                        emblem = true,
                    ),
                )
            )
        }
        description = "Chandra, Roaring Flame deals 6 damage to each opponent. Each player dealt damage this " +
            "way gets an emblem with \"$CHANDRA_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "135"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/back/b/0/b0d6caf0-4fa8-4ec5-b7f4-1307474d1b13.jpg?1783938336"

        ruling("2015-06-22", "The emblem created by Chandra, Roaring Flame is colorless. The damage it deals is from a colorless source.")
        ruling("2015-06-22", "Only players actually dealt damage by the third ability of Chandra, Roaring Flame will get an emblem. If all of that damage to a player is prevented, that player won't get an emblem. If any of that damage is redirected to Chandra's controller, that player will get an emblem.")
        ruling("2015-06-22", "Each player who gets Chandra's emblem is the owner of that emblem. In multiplayer games, that emblem will remain in the game as long as its owner does, even if Chandra's owner leaves the game.")
    }
}

val ChandraFireOfKaladesh: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ChandraFireOfKaladeshFront,
    backFace = ChandraRoaringFlame,
)
