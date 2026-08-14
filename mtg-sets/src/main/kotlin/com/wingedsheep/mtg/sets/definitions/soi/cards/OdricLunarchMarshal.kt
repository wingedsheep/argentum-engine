package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Odric, Lunarch Marshal — Shadows over Innistrad #31
 * {3}{W} · Legendary Creature — Human Soldier · 3/3
 *
 * At the beginning of each combat, creatures you control gain first strike until end of turn if a
 * creature you control has first strike. The same is true for flying, deathtouch, double strike,
 * haste, hexproof, indestructible, lifelink, menace, reach, skulk, trample, and vigilance.
 *
 * Modeling notes:
 *
 *  - **"Each combat", not "your combat"** — [Triggers.EachCombat] (`Player.Each`), so Odric also
 *    hands out keywords on opponents' turns. That is the whole point of the card in a blocking
 *    stance; [Triggers.BeginCombat] would silently halve it.
 *  - **The keyword list is one shared loop.** Each of the printed keywords is an independent
 *    "if you control a creature with K, creatures you control gain K" clause, so the card is a
 *    [Effects.Composite] over [SHARED_KEYWORDS] rather than thirteen hand-written blocks. Granting
 *    K never changes whether a creature has some *other* keyword J, so evaluating the clauses in
 *    sequence is equivalent to evaluating them simultaneously — there is no ordering hazard to
 *    guard against.
 *  - **The gate is resolution-time, not continuous.** Each clause is a [ConditionalEffect] (which
 *    lowers to a `Gate.WhenCondition` state test) rather than the `condition` parameter on
 *    `GrantKeyword`. That parameter is re-evaluated on *every* projection, which would let the
 *    granted keywords blink out the moment the creature that supplied them left the battlefield.
 *    The printed ruling is the opposite: "the abilities gained won't change even if every creature
 *    that normally had the abilities leaves the battlefield."
 *  - **[Effects.ForEachInGroup], not a `GroupRef` target.** A `GroupRef` on `GrantKeyword` is not
 *    expanded per-permanent (see The Wind Crystal), so the grant is fanned out over the group and
 *    each creature receives its own floating end-of-turn keyword effect. This also snapshots the
 *    affected set at resolution, matching the ruling that creatures you come to control later in
 *    the turn gain nothing.
 *  - **Skulk is omitted** — it is the one keyword on the printed list with no
 *    [com.wingedsheep.sdk.core.Keyword] entry and no blocking rule in the engine, so nothing in the
 *    card pool can currently *have* skulk and the "if a creature you control has skulk" clause can
 *    never be satisfied. The twelve modelled clauses are therefore exact in every reachable game
 *    state. Adding skulk is `add-feature` work (a `Keyword` value, a
 *    `CantBeBlockedByCreaturesWithGreaterPower` static ability mirroring the existing
 *    `CantBeBlockedByCreaturesWithLessPower`, a `BlockEvasionRule`, and client keyword display);
 *    when it lands, add `Keyword.SKULK` to [SHARED_KEYWORDS] and this card is complete.
 */
private val SHARED_KEYWORDS = listOf(
    Keyword.FIRST_STRIKE,
    Keyword.FLYING,
    Keyword.DEATHTOUCH,
    Keyword.DOUBLE_STRIKE,
    Keyword.HASTE,
    Keyword.HEXPROOF,
    Keyword.INDESTRUCTIBLE,
    Keyword.LIFELINK,
    Keyword.MENACE,
    Keyword.REACH,
    // Keyword.SKULK — not yet in the engine; see the class doc.
    Keyword.TRAMPLE,
    Keyword.VIGILANCE,
)

val OdricLunarchMarshal = card("Odric, Lunarch Marshal") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "At the beginning of each combat, creatures you control gain first strike until " +
        "end of turn if a creature you control has first strike. The same is true for flying, " +
        "deathtouch, double strike, haste, hexproof, indestructible, lifelink, menace, reach, " +
        "skulk, trample, and vigilance."

    triggeredAbility {
        trigger = Triggers.EachCombat
        effect = Effects.Composite(
            SHARED_KEYWORDS.map { keyword ->
                ConditionalEffect(
                    condition = Conditions.ControlCreatureWithKeyword(keyword),
                    effect = Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Creature.youControl()),
                        Effects.GrantKeyword(keyword, EffectTarget.Self, Duration.EndOfTurn)
                    )
                )
            },
            descriptionOverride = "Creatures you control gain first strike until end of turn if a " +
                "creature you control has first strike. The same is true for flying, deathtouch, " +
                "double strike, haste, hexproof, indestructible, lifelink, menace, reach, " +
                "trample, and vigilance."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c77c30f-d813-46e6-9cdd-938b4a6359ad.jpg?1783937814"

        ruling("2016-04-08", "Odric's ability triggers at the beginning of each combat, not just combat on your turn, whether or not any creatures you control have any of the listed abilities. If a creature gains one of the listed abilities before Odric's triggered ability resolves, perhaps due to another ability that triggered at the beginning of combat, then creatures you control will gain that ability.")
        ruling("2016-04-08", "The set of creatures affected by Odric's ability and how they are affected is determined as the ability resolves. Creatures you begin to control later in the turn won't gain any abilities or cause creatures to gain new abilities, and the abilities gained won't change even if every creature that normally had the abilities leaves the battlefield.")
        ruling("2016-04-08", "Multiple instances of any of the abilities Odric can grant your creatures are redundant.")
        ruling("2020-01-24", "If one of those creatures has one or more variants of the listed keywords (for example, hexproof from white), creatures you control gain those specific variants.")
    }
}
