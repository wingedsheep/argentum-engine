package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Athreos, God of Passage — Journey into Nyx #146
 * {1}{W}{B} · Legendary Enchantment Creature — God · 5/4
 * Indestructible
 * As long as your devotion to white and black is less than seven, Athreos isn't a creature.
 * Whenever another creature you own dies, return it to your hand unless target opponent pays 3 life.
 *
 * The devotion clause is the two-colour God template ([IroasGodOfVictory]).
 *
 * "Another creature you own" is ownership, not control — a creature you own dying under an
 * opponent's control still triggers (ruling) — so the subject is `Creature.ownedByYou()`.
 * "Unless target opponent pays 3 life" is [Effects.PayOrSuffer] offered to that targeted opponent
 * as the ability resolves (Tempest Efreet); a player with less than 3 life can't pay (CR 119.4). The
 * return reads the dying card through the trigger and is gated on it still being in the graveyard,
 * so a card that left the graveyard in response stays where it is (ruling), and a token — which has
 * ceased to exist — returns nothing even though the opponent is still offered the choice (ruling).
 * With no legal opponent to target the ability is removed from the stack (ruling).
 */
val AthreosGodOfPassage = card("Athreos, God of Passage") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Enchantment Creature — God"
    power = 5
    toughness = 4
    oracleText = "Indestructible\n" +
        "As long as your devotion to white and black is less than seven, Athreos isn't a creature.\n" +
        "Whenever another creature you own dies, return it to your hand unless target opponent pays 3 life."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.WHITE, Color.BLACK),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.ownedByYou()).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.PayLife(3),
            suffer = Effects.Move(EffectTarget.TriggeringEntity, Zone.HAND, fromZone = Zone.GRAVEYARD),
            player = opponent,
        )
        description = "Whenever another creature you own dies, return it to your hand unless target " +
            "opponent pays 3 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "146"
        artist = "Ryan Barger"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52705c53-883e-4b6a-9c08-3fa35f6f17d5.jpg?1783939406"
        ruling(
            "2014-04-26",
            "Athreos's last ability will trigger if a token creature you own dies. The target opponent has the " +
                "option to pay 3 life, although the token can't return to your hand."
        )
        ruling(
            "2014-04-26",
            "It doesn't matter who controlled the creature when it died. Athreos's last ability will trigger if " +
                "you owned that creature."
        )
        ruling(
            "2014-04-26",
            "The target opponent chooses whether to pay 3 life when Athreos's last ability resolves. You won't " +
                "return the card to your hand if that player pays 3 life or if the card leaves the graveyard " +
                "before the ability resolves."
        )
        ruling(
            "2014-04-26",
            "If there are no legal targets for Athreos's last ability (perhaps because each of your opponents " +
                "has hexproof), it will be removed from the stack with no effect. No one may pay 3 life and you " +
                "won't return the creature card to your hand."
        )
        ruling(
            "2020-01-24",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
    }
}
