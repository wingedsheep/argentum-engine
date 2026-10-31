package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mythos of Snapdax — Ikoria: Lair of Behemoths #24
 * {2}{W}{W} · Sorcery
 *
 * Each player chooses an artifact, a creature, an enchantment, and a planeswalker from among the
 * nonland permanents they control, then sacrifices the rest. If {B}{R} was spent to cast this
 * spell, you choose the permanents for each player instead.
 *
 * Per the rulings the choices go starting with you, then each other player in turn order, and
 * every unchosen nonland permanent is sacrificed at the same time once all choices are made. Each
 * category is a separate one-of choice over that player's nonland permanents, so an artifact
 * creature can be kept as both the artifact and the creature. Lands (even artifact lands) are never
 * candidates and never sacrificed. The {B}{R} check reads the mana actually spent; it only swaps
 * who makes each player's choices ([Chooser.SourceController] instead of the iterated player).
 */
val MythosOfSnapdax = card("Mythos of Snapdax") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "BRW"
    typeLine = "Sorcery"
    oracleText = "Each player chooses an artifact, a creature, an enchantment, and a planeswalker from " +
        "among the nonland permanents they control, then sacrifices the rest. If {B}{R} was spent to " +
        "cast this spell, you choose the permanents for each player instead."

    spell {
        effect = Effects.If(
            condition = Conditions.ManaSpentToCastIncludes(requiredBlack = 1, requiredRed = 1),
            then = snapdaxKeepOneOfEach(Chooser.SourceController),
            otherwise = snapdaxKeepOneOfEach(Chooser.Controller),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2712a1a3-dd28-44c8-a661-5bcf68d3acaa.jpg?1783931085"
        ruling(
            "2020-04-17",
            "If you control a permanent with more than one type, you may choose it for each of those types. " +
                "For example, you could choose an artifact creature as the artifact you keep and also as the " +
                "creature you keep."
        )
        ruling(
            "2020-04-17",
            "If you're choosing the permanents for each player, your choices must still be legal. For " +
                "example, you can't decline to choose a creature a player will keep if that player controls " +
                "a creature."
        )
        ruling(
            "2020-04-17",
            "As Mythos of Snapdax resolves, first you choose which permanents you control won't be " +
                "sacrificed, then each other player in turn order does the same, knowing the choices made " +
                "before them. Then each nonland permanent not chosen is sacrificed at the same time."
        )
        ruling("2020-04-17", "Lands with another permanent type can't be chosen and won't be sacrificed.")
        ruling(
            "2020-04-17",
            "The abilities of the Mythos check what colors of mana were spent to cast the spell. It's not an " +
                "alternative cost to cast the spell."
        )
        ruling(
            "2020-04-17",
            "If an effect copies the Mythos spell, no mana was spent to cast the copy, so the copy won't " +
                "receive the bonus."
        )
        ruling(
            "2020-04-17",
            "The ability checks what mana was actually spent to cast a spell. If an effect allows you to " +
                "spend mana \"as though it were mana\" of any color or type, that allows you to spend mana " +
                "you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell."
        )
    }
}

/**
 * For each player (starting with you): [chooser] picks one artifact, one creature, one enchantment
 * and one planeswalker among that player's nonland permanents. After every player is done, all
 * unpicked nonland permanents are sacrificed together.
 */
private fun snapdaxKeepOneOfEach(chooser: Chooser): Effect = Effects.Pipeline {
    val categories = listOf(
        GameObjectFilter.Artifact to "an artifact",
        GameObjectFilter.Creature to "a creature",
        GameObjectFilter.Enchantment to "an enchantment",
        GameObjectFilter.Planeswalker to "a planeswalker",
    )
    val collected = forEachPlayerCollecting(Player.EachStartingWithYou) {
        val nonland = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.NonlandPermanent))
        val kept = categories.map { (category, label) ->
            chooseExactly(
                1,
                from = filter(nonland, category),
                chooser = chooser,
                prompt = "Choose $label to keep. Unchosen nonland permanents are sacrificed.",
                selectedLabel = "Keep",
                remainderLabel = "Sacrifice",
                useTargetingUI = true,
            )
        }
        listOf(nonland) + kept
    }
    val atRisk = collected.first()
    val doomed = collected.drop(1).fold(atRisk) { remaining, kept -> exclude(remaining, kept) }
    sacrifice(doomed)
}
