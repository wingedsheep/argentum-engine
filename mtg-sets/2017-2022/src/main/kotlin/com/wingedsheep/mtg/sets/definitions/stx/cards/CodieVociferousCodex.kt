package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Codie, Vociferous Codex — Strixhaven #253 (canonical printing)
 * {3} · Legendary Artifact Creature — Book Construct · 1/4
 *
 * You can't cast permanent spells.
 * {4}, {T}: Add {W}{U}{B}{R}{G}. When you next cast a spell this turn, exile cards from the top
 * of your library until you exile an instant or sorcery card with lesser mana value. Until end of
 * turn, you may cast that card without paying its mana cost. Put each other card exiled this way
 * on the bottom of your library in a random order.
 *
 * The drawback is [PlayersCantCastSpells] turned inward (Grid Monitor's shape) with the permanent
 * filter; the engine judges it against the face being cast, so an Adventure's instant/sorcery half
 * or an MDFC's instant/sorcery face stays castable (the second ruling).
 *
 * The second ability is a mana ability (Crystal Quarry's five `AddMana`s) whose non-mana rider is a
 * one-shot "when you next cast a spell this turn" delayed trigger (Summon: Brynhildr's Gestalt Mode
 * shape). It runs on manual activation and on auto-tap alike, so activating it while paying for a
 * spell makes that spell "the next spell" (the third ruling). The trigger's body is Jodah, the
 * Unifier's "lesser mana value" walk, except the hit isn't cast on resolution: it is granted a
 * cast-from-exile-for-free permission until end of turn (The Infamous Cruelclaw's grant pair), with
 * normal timing. Only the misses go to the bottom; the hit stays in exile if never cast.
 */
val CodieVociferousCodex = card("Codie, Vociferous Codex") {
    manaCost = "{3}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Artifact Creature — Book Construct"
    power = 1
    toughness = 4
    oracleText = "You can't cast permanent spells.\n" +
        "{4}, {T}: Add {W}{U}{B}{R}{G}. When you next cast a spell this turn, exile cards from the " +
        "top of your library until you exile an instant or sorcery card with lesser mana value. " +
        "Until end of turn, you may cast that card without paying its mana cost. Put each other " +
        "card exiled this way on the bottom of your library in a random order."

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.You,
            spellFilter = GameObjectFilter.Permanent,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.AddMana(Color.WHITE, 1) then
            Effects.AddMana(Color.BLUE, 1) then
            Effects.AddMana(Color.BLACK, 1) then
            Effects.AddMana(Color.RED, 1) then
            Effects.AddMana(Color.GREEN, 1) then
            Effects.CreateDelayedTrigger(
                trigger = Triggers.you.casts(),
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.Pipeline {
                    val lesserSpell = GameObjectFilter.InstantOrSorcery.manaValueAtMostDynamic(
                        DynamicAmounts.triggeringSpellManaValue() - 1
                    )
                    val (found, revealed) = gatherUntilMatch(lesserSpell)
                    exile(revealed)
                    run(Effects.GrantMayPlayFromExile(found, expiry = MayPlayExpiry.EndOfTurn))
                    run(Effects.GrantPlayWithoutPayingCost(found, expiry = MayPlayExpiry.EndOfTurn))
                    toLibraryBottom(exclude(revealed, found), order = CardOrder.Random)
                },
            )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{4}, {T}: Add {W}{U}{B}{R}{G}. When you next cast a spell this turn, exile " +
            "cards from the top of your library until you exile an instant or sorcery card with " +
            "lesser mana value. Until end of turn, you may cast that card without paying its mana " +
            "cost. Put each other card exiled this way on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "253"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea476ee1-67d9-4dd8-a5ac-f68a155eb18b.jpg?1783927281"

        ruling("2021-04-16", "A permanent spell is an artifact, creature, enchantment, or planeswalker spell. You may still play lands and cast instant and sorcery spells. Other effects can still put permanents onto the battlefield.")
        ruling("2021-04-16", "While you control Codie (as much as anyone can really \"control\" Codie), you may cast any face of a modal double-faced card that's an instant or sorcery face, even if the other face is a permanent face.")
        ruling("2021-04-16", "Codie's activated ability is a mana ability. It can't be countered or responded to, and you can activate it while casting a spell. If you do, that spell is \"the next spell\" you cast this turn.")
        ruling("2021-04-16", "The delayed triggered ability created by Codie's second ability will resolve before the spell that caused it to trigger. That ability will resolve even if the spell is countered or otherwise doesn't resolve.")
        ruling("2021-04-16", "The last card exiled is not cast immediately. You may cast it until the end of the current turn. If you do not cast it, that card will remain in exile.")
        ruling("2021-04-16", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2021-04-16", "If you exile your entire library without exiling an instant or sorcery card with a lesser mana value, you will randomize the exiled cards and they again become your library, ending the effect. You will not continue to exile and shuffle your library forever.")
    }
}
