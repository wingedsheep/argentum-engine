package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Combustible Gearhulk
 * {4}{R}{R}
 * Artifact Creature — Construct
 * 6/6
 * First strike
 * When this creature enters, target opponent may have you draw three cards. If the player
 * doesn't, you mill three cards, then this creature deals damage to that player equal to the
 * total mana value of those cards.
 *
 * Same shape as Palantír of Orthanc: the "may" is routed to the targeted opponent through
 * [Effects.May]'s `decisionMaker`; the else-branch mills into the "milled" collection and the
 * damage reads its total mana value ({X} counts as 0).
 */
val CombustibleGearhulk = card("Combustible Gearhulk") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Construct"
    power = 6
    toughness = 6
    oracleText = "First strike\nWhen this creature enters, target opponent may have you draw three " +
        "cards. If the player doesn't, you mill three cards, then this creature deals damage to that " +
        "player equal to the total mana value of those cards."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.May(
            effect = Effects.DrawCards(3, EffectTarget.Controller),
            descriptionOverride = "Have Combustible Gearhulk's controller draw three cards?",
            decisionMaker = opponent,
            otherwise = Patterns.Library.mill(3, EffectTarget.Controller) then
                Effects.DealDamage(DynamicAmounts.manaValueSumOf(Patterns.Library.milled), opponent)
        )
        description = "When this creature enters, target opponent may have you draw three cards. If " +
            "the player doesn't, you mill three cards, then this creature deals damage to that player " +
            "equal to the total mana value of those cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "112"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0f43147-4552-48fd-be0a-0629b9a0ad69.jpg?1783937195"
        ruling("2016-09-20", "While Combustible Gearhulk's triggered ability is resolving, no player may take any actions other than those specified by the Gearhulk. Once the opponent makes a choice, players can't cast spells or activate abilities until after the cards are drawn or put into the graveyard and damage has been dealt (if applicable).")
        ruling("2016-09-20", "If you have fewer than three cards in your library, the opponent may choose to have you draw three cards. If so, you'll lose the game as state-based actions are performed.")
        ruling("2016-09-20", "If any of the cards put into your graveyard have {X} in their mana cost, X is considered to be 0.")
    }
}
