package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bilbo's Gambit — The Hobbit #5
 * {1}{W} · Instant · Rare
 *
 * Gift a Treasure (You may promise an opponent a gift as you cast this spell. If you do, they
 * create a Treasure token before its other effects. It's an artifact with "{T}, Sacrifice this
 * token: Add one mana of any color.")
 * Return target spell to its owner's hand. If the gift was promised, players can't cast spells
 * this turn.
 *
 * Modeling notes:
 *  - Gift on an instant has no permanent to trigger off, so it is the two-mode
 *    [Patterns.Mechanic.giftSpell] shape rather than the `gift(GiftKind)` keyword — `CardValidator`
 *    rejects the keyword on a non-permanent for exactly that reason. Mode 0 is "don't promise",
 *    mode 1 promises and picks the recipient (CR 702.174a).
 *  - Both modes target a spell, so the bounce happens either way; only the lockout rides on the
 *    promise. The Treasure lands *before* the bounce, matching "they create a Treasure token
 *    before its other effects" (CR 702.174d).
 *  - "Players can't cast spells this turn" is every player, including you — the gambit shuts the
 *    turn down after the bounce, so the returned spell can't simply be recast. [Player.Each]
 *    covers all of them; the restriction is [Duration.EndOfTurn] by default.
 *  - The bounce resolving *before* the lockout matters: were the order reversed the spell would
 *    still return, but the intervening state would be identical. Ordering follows the printed text.
 */
val BilbosGambit = card("Bilbo's Gambit") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Gift a Treasure (You may promise an opponent a gift as you cast this spell. " +
        "If you do, they create a Treasure token before its other effects. It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Return target spell to its owner's hand. If the gift was promised, players can't cast " +
        "spells this turn."

    spell {
        effect = Patterns.Mechanic.giftSpell(
            noGiftMode = Mode.withTarget(
                Effects.ReturnSpellToOwnersHand(),
                Targets.Spell,
                "Don't promise a gift — return target spell to its owner's hand"
            ),
            giftMode = Mode.withTarget(
                Effects.CreateTreasure(
                    count = 1,
                    controller = EffectTarget.PlayerRef(Player.ChosenOpponent)
                )
                    .then(Effects.ReturnSpellToOwnersHand())
                    .then(Effects.CantCastSpells(EffectTarget.PlayerRef(Player.Each)))
                    .then(Effects.GiftGiven()),
                Targets.Spell,
                "Promise a gift — that opponent creates a Treasure, then return target spell to " +
                    "its owner's hand and players can't cast spells this turn"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45ad01f0-cda8-4931-82bb-cb4949e56ae9.jpg?1784894818"
    }
}
