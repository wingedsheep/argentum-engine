package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Perch Protection — Bloomburrow Commander #11 (canonical printing)
 * {4}{W}{W} · Instant
 *
 * Gift an extra turn (You may promise an opponent a gift as you cast this spell. If you do, they take
 * an extra turn after this one.)
 * Create four 2/2 blue Bird creature tokens with flying. If the gift was promised, all permanents you
 * control phase out, and until your next turn, your life total can't change and you gain protection
 * from everything.
 * Exile Perch Protection.
 *
 * - Gift on an instant is `Patterns.Mechanic.giftSpell`: the gift mode gives the chosen opponent the
 *   extra turn first (the gift is given before the spell's other effects), then runs the rest.
 * - The gift rider is Teferi's Protection's composition: both life locks + `GrantPlayerProtection`
 *   until your next turn, and every permanent you control phases out — including the four Birds just
 *   made, which come back with everything else at your untap step. "Until your next turn" spans the
 *   opponent's extra turn.
 * - "Exile Perch Protection" replaces the graveyard as its destination on resolution.
 */
val PerchProtection = card("Perch Protection") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Gift an extra turn (You may promise an opponent a gift as you cast this spell. If you do, " +
        "they take an extra turn after this one.)\nCreate four 2/2 blue Bird creature tokens with flying. " +
        "If the gift was promised, all permanents you control phase out, and until your next turn, your life " +
        "total can't change and you gain protection from everything.\nExile Perch Protection."

    val birds = Effects.CreateToken(
        power = 2,
        toughness = 2,
        colors = setOf(Color.BLUE),
        creatureTypes = setOf("Bird"),
        keywords = setOf(Keyword.FLYING),
        count = 4,
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0796ccd-b8ba-49c9-b76b-4fce55c242dc.jpg?1783909759"
    )

    spell {
        effect = Patterns.Mechanic.giftSpell(
            Mode.noTarget(birds, "Don't promise a gift — create four 2/2 blue Bird tokens with flying"),
            Mode.noTarget(
                Effects.TakeExtraTurn(EffectTarget.PlayerRef(Player.ChosenOpponent)) then
                    birds then
                    Effects.ForEachInGroup(
                        Filters.Group.permanentsYouControl,
                        Effects.PhaseOut(EffectTarget.IterationEntity)
                    ) then
                    Effects.LockLifeGain(EffectTarget.Controller, Duration.UntilYourNextTurn) then
                    Effects.LockLifeLoss(EffectTarget.Controller, Duration.UntilYourNextTurn) then
                    Effects.GrantPlayerProtection(
                        target = EffectTarget.Controller,
                        scope = ProtectionScope.Everything,
                        duration = Duration.UntilYourNextTurn
                    ) then
                    Effects.GiftGiven(),
                "Promise a gift — an opponent takes an extra turn; create four Birds, phase out your " +
                    "permanents, and until your next turn your life total can't change and you have " +
                    "protection from everything"
            )
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f81d6ea-9a76-43b9-b030-d40c5cfbeefe.jpg?1783910735"
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "Gaining protection from everything causes a spell or ability on the stack to have an illegal target if it targets you.")
        ruling("2024-07-26", "If a token is phased out, it will phase in as your next untap step begins.")
        ruling("2024-07-26", "If your untap step is somehow skipped as your next turn begins, your phased-out permanents won't phase in until the next untap step you actually have, but you'll no longer have protection from everything and your life total can change again.")
    }
}
