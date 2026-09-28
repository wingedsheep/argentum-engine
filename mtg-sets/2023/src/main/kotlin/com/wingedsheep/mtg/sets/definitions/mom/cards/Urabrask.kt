package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urabrask // The Great Work — March of the Machine #169
 * {2}{R}{R} · Legendary Creature — Phyrexian Praetor 4/4 // Enchantment — Saga
 *
 * Front — Urabrask:
 *   First strike
 *   Whenever you cast an instant or sorcery spell, Urabrask deals 1 damage to target opponent.
 *   Add {R}.
 *   {R}: Exile Urabrask, then return it to the battlefield transformed under its owner's control.
 *   Activate only as a sorcery and only if you've cast three or more instant and/or sorcery
 *   spells this turn.
 *
 * Back — The Great Work:
 *   I — This Saga deals 3 damage to target opponent and each creature they control.
 *   II — Create three Treasure tokens.
 *   III — Until end of turn, you may cast instant and sorcery spells from any graveyard. If a spell
 *   cast this way would be put into a graveyard, exile it instead. Exile this Saga, then return it
 *   to the battlefield (front face up).
 *
 * Chapter III's permission is a [MayCastFromGraveyard] with `fromAnyGraveyard` and
 * `exileInsteadOfGraveyard`, granted to the *controller* rather than the Saga: the same resolution
 * exiles the Saga and returns it as a new object, and a permission anchored to the Saga would not
 * outlive that. The trigger's "Add {R}" is not a mana ability — it rides the stack (ruling).
 */
private val TheGreatWork = card("The Great Work") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — This Saga deals 3 damage to target opponent and each creature they control.\n" +
        "II — Create three Treasure tokens.\n" +
        "III — Until end of turn, you may cast instant and sorcery spells from any graveyard. " +
        "If a spell cast this way would be put into a graveyard, exile it instead. Exile this " +
        "Saga, then return it to the battlefield (front face up)."

    // I — This Saga deals 3 damage to target opponent and each creature they control.
    sagaChapter(1) {
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(3, opponent) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent)),
            Effects.DealDamage(3, EffectTarget.IterationEntity)
        )
    }

    // II — Create three Treasure tokens.
    sagaChapter(2) {
        effect = Effects.CreateTreasure(3)
    }

    // III — cast instants and sorceries from any graveyard this turn, exiled instead of returning;
    // then the Saga flips back to Urabrask.
    sagaChapter(3) {
        effect = Effects.GrantStaticAbility(
            MayCastFromGraveyard(
                filter = GameObjectFilter.InstantOrSorcery,
                exileInsteadOfGraveyard = true,
                fromAnyGraveyard = true
            ),
            target = EffectTarget.Controller,
            duration = Duration.EndOfTurn
        ) then Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "169"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/back/7/1/712fb9e5-bd67-4173-a2d4-061aeb6253b5.jpg?1783916984"
    }
}

private val UrabraskFront = card("Urabrask") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    oracleText = "First strike\n" +
        "Whenever you cast an instant or sorcery spell, Urabrask deals 1 damage to target opponent. " +
        "Add {R}.\n" +
        "{R}: Exile Urabrask, then return it to the battlefield transformed under its owner's " +
        "control. Activate only as a sorcery and only if you've cast three or more instant and/or " +
        "sorcery spells this turn."
    power = 4
    toughness = 4
    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val opponent = target(Targets.Opponent)
        effect = Effects.DealDamage(1, opponent) then Effects.AddMana(Color.RED, 1)
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouCastSpellsThisTurn(3, GameObjectFilter.InstantOrSorcery)
            )
        )
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "169"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/712fb9e5-bd67-4173-a2d4-061aeb6253b5.jpg?1783916984"
        ruling(
            "2023-04-14",
            "Urabrask's second ability isn't a mana ability, even though it causes you to add mana. " +
                "It uses the stack and can be responded to."
        )
        ruling(
            "2023-04-14",
            "Urabrask will count any instant or sorcery spells you've cast during the turn, whether " +
                "those spells resolved, didn't resolve, were countered, or left the stack some other " +
                "way. Urabrask won't count copies of instant and sorcery spells that were created on " +
                "the stack and not cast."
        )
        ruling(
            "2023-04-14",
            "After chapter III of The Great Work resolves, casting spells from graveyards follows the " +
                "normal rules for casting those cards. You must pay their costs, and you must follow " +
                "all applicable timing rules. For example, to cast a sorcery spell from a graveyard, " +
                "you can do so by paying its mana cost only during your main phase while the stack is " +
                "empty."
        )
        ruling(
            "2023-04-14",
            "If multiple players are allowed to cast the same card, the player with priority at any " +
                "given time determines who can cast it. The player whose turn it is has priority as " +
                "each step and phase begins, and they receive priority after each spell and ability " +
                "resolves. This means immediately after chapter III resolves you'll have priority to " +
                "cast spells before any other player does."
        )
    }
}

val Urabrask: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = UrabraskFront,
    backFace = TheGreatWork,
)
