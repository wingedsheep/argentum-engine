package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kaya, Intangible Slayer — Phyrexia: All Will Be One #205
 * {3}{W}{W}{B}{B} · Legendary Planeswalker — Kaya · Rare · Loyalty 6
 *
 * Hexproof
 * +2: Each opponent loses 3 life and you gain 3 life.
 * 0: You draw two cards. Then each opponent may scry 1.
 * −3: Exile target creature or enchantment. If it wasn't an Aura, create a token that's a copy of
 *     it, except it's a 1/1 white Spirit creature with flying in addition to its other types.
 *
 * Modeling:
 * - "Each opponent may scry 1" is the Myr Custodian shape: `ForEachPlayer(EachOpponent)` rebinds
 *   the resolving controller to each opponent in turn, so the yes/no and the scry belong to them.
 * - "If it wasn't an Aura" asks about the permanent as it last existed on the battlefield (per the
 *   ruling, a bestowed creature that was an Aura gets no copy). The check is made on the live
 *   target immediately before it's exiled — nothing can change it between the two — and selects
 *   between "exile, then copy" and a plain exile. The token copy reads the exiled object's
 *   copiable values; "in addition to its other types" keeps the copied card types and subtypes and
 *   adds Creature and Spirit, while the colour and base P/T are overridden.
 */
val KayaIntangibleSlayer = card("Kaya, Intangible Slayer") {
    manaCost = "{3}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Kaya"
    startingLoyalty = 6
    oracleText = "Hexproof\n" +
        "+2: Each opponent loses 3 life and you gain 3 life.\n" +
        "0: You draw two cards. Then each opponent may scry 1.\n" +
        "−3: Exile target creature or enchantment. If it wasn't an Aura, create a token that's a " +
        "copy of it, except it's a 1/1 white Spirit creature with flying in addition to its other types."

    keywords(Keyword.HEXPROOF)

    loyaltyAbility(+2) {
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(3)
    }

    loyaltyAbility(0) {
        effect = Effects.DrawCards(2) then
            Effects.ForEachPlayer(
                Player.EachOpponent,
                listOf(
                    Effects.May(
                        effect = Effects.Scry(1),
                        decisionMaker = EffectTarget.PlayerRef(Player.You),
                        descriptionOverride = "Scry 1?",
                    ),
                ),
            )
    }

    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.CreatureOrEnchantment)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Any.notSubtype(Subtype.AURA),
                permanent,
            ),
            // The copy is taken before the exile: once exiled, the object has lost any copy effect
            // or face-down status it had on the battlefield (CR 400.7), and the token must copy
            // the permanent as it last existed there.
            then = Effects.CreateTokenCopyOfTarget(
                    target = permanent,
                    overridePower = 1,
                    overrideToughness = 1,
                    overrideColors = setOf(Color.WHITE),
                    addedSubtypes = setOf(Subtype("Spirit")),
                    addCardTypes = setOf(CardType.CREATURE.name),
                    addedKeywords = setOf(Keyword.FLYING),
                ) then Effects.Exile(permanent),
            otherwise = Effects.Exile(permanent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6865b8c7-b260-48e8-a905-64cbf06b7e57.jpg?1783918001"
        ruling("2023-02-04", "For the middle loyalty ability, each opponent in turn order chooses whether or not to scry. Those who do (likely all of them) look at the top card of their library at the same time, then they decide in turn order where their card goes. Each opponent will know the choices of previous players in turn order before making their own choices.")
        ruling("2023-02-04", "Except for the listed exceptions, the token copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or it is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, and so on.")
        ruling("2023-02-04", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2023-02-04", "If the copied creature is a token, the new token that's created copies the original characteristics of that token as stated by the effect that created that token, with the exceptions noted above.")
        ruling("2023-02-04", "Kaya's last ability checks to see whether the permanent was an Aura when it last existed on the battlefield, not whether it is an Aura card in exile. For example, if an enchantment creature with the bestow ability (an ability that allows a creature card to be cast as an Aura instead) is an Aura and the target of Kaya's last ability, the ability will not create a copy of it.")
    }
}
