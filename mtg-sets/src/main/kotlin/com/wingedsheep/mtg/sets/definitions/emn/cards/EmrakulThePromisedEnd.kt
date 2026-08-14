package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Emrakul, the Promised End
 * {13}
 * Legendary Creature — Eldrazi
 * 13/13
 *
 * This spell costs {1} less to cast for each card type among cards in your graveyard.
 * When you cast this spell, you gain control of target opponent during that player's next turn.
 * After that turn, that player takes an extra turn.
 * Flying, trample, protection from instants
 *
 * Implementation notes:
 * - The cost reduction is a self-cast [ModifySpellCost] over
 *   [CostReductionSource.CardTypesInYourGraveyard] — distinct *types*, not cards, so nine creature
 *   cards in the graveyard still only shave {1}. Emrakul's cost is all generic, and the generic
 *   reduction rail floors at {0}, so the maximum discount is {9} (2025-01-24 ruling: the nine card
 *   types that can sit in a graveyard).
 * - "Protection from instants" is the printed keyword [ProtectionScope.CardType], projected as
 *   `PROTECTION_FROM_CARDTYPE_INSTANT`. Per the rulings this only bites while Emrakul is on the
 *   battlefield — a spell that targets it on the stack (Syncopate) is unaffected, which falls out of
 *   the enforcement sites all gating on battlefield membership.
 * - The cast trigger is [Triggers.WhenYouCastThisSpell] (CR 603.2 — it resolves *before* Emrakul and
 *   still resolves if Emrakul is countered), targeting an opponent once and feeding both halves:
 *   [Effects.HijackNextTurn] takes over that player's next turn, and [Effects.TakeExtraTurn] hands
 *   them the turn after it. The extra turn is modelled the engine's usual way — every other player
 *   skips their next turn — so in a two-player game the hijacked turn is followed directly by the
 *   opponent's extra turn, which is exactly the printed sequence.
 */
val EmrakulThePromisedEnd = card("Emrakul, the Promised End") {
    manaCost = "{13}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 13
    toughness = 13
    oracleText = "This spell costs {1} less to cast for each card type among cards in your graveyard.\n" +
        "When you cast this spell, you gain control of target opponent during that player's next " +
        "turn. After that turn, that player takes an extra turn.\n" +
        "Flying, trample, protection from instants"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardTypesInYourGraveyard()
            )
        )
    }

    keywords(Keyword.FLYING, Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Instant")))

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            Effects.HijackNextTurn(opponent),
            Effects.TakeExtraTurn(target = opponent)
        )
        description = "When you cast this spell, you gain control of target opponent during that " +
            "player's next turn. After that turn, that player takes an extra turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Jaime Jones"
        flavorText = "An enigma as vexing as life itself."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d74a469-c71d-4773-99d3-5456b31df424.jpg?1783937526"

        ruling(
            "2025-01-24",
            "The card types that could appear in your graveyard are artifact, battle, creature, " +
                "enchantment, instant, kindred, land, planeswalker, and sorcery. Supertypes (such " +
                "as legendary and basic) and subtypes (such as Human and Equipment) are not " +
                "counted. The maximum discount that Emrakul's own ability can provide is {9}."
        )
        ruling(
            "2025-01-24",
            "Protection from instants means that Emrakul can't be the target of instant spells or " +
                "activated or triggered abilities from instant cards, and damage that would be " +
                "dealt to it by instant spells or cards is prevented. Instant spells may still " +
                "affect it in other ways; for example, it would still receive the bonus from Rally " +
                "the Peasants."
        )
        ruling(
            "2025-01-24",
            "Protection abilities only apply while the object with the ability is on the " +
                "battlefield. Notably, Emrakul may be the target of a spell that targets it while " +
                "on the stack, such as Syncopate."
        )
        ruling(
            "2025-01-24",
            "An ability that triggers when a player casts a spell resolves before the spell that " +
                "caused it to trigger. It resolves even if that spell is countered or otherwise " +
                "leaves the stack without resolving."
        )
        ruling("2025-01-24", "The player you're controlling is still the active player during that turn.")
        ruling(
            "2025-01-24",
            "You only control the player. You don't control any of that player's permanents, " +
                "spells, or abilities."
        )
        ruling(
            "2025-01-24",
            "While controlling another player, you make all choices and decisions that player is " +
                "allowed to make or is told to make during that turn. This includes choices about " +
                "what spells to cast or what abilities to activate, as well as any decisions " +
                "called for by triggered abilities or for any other reason."
        )
        ruling(
            "2025-01-24",
            "You can use only the affected player's resources (cards, mana, and so on) to pay " +
                "costs for that player; you can't use your own. Similarly, you can use the " +
                "affected player's resources only to pay that player's costs; you can't spend " +
                "them on your costs."
        )
        ruling(
            "2025-01-24",
            "While controlling another player, you can see all cards in the game that player can " +
                "see. This includes cards in that player's hand, face-down cards that player " +
                "controls, and any cards in that player's library the player may look at."
        )
        ruling(
            "2025-01-24",
            "If the targeted player skips their next turn, you'll control the next turn the " +
                "affected player actually takes, and the extra turn the player takes will be " +
                "after that turn."
        )
    }
}
