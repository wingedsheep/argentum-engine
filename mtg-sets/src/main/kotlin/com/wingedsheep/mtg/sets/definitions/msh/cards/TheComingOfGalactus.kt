package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * The Coming of Galactus — Marvel Super Heroes #212
 * {2}{B}{B}{G} · Enchantment — Saga
 *
 * I — Destroy up to one target nonland permanent.
 * II, III — Each opponent loses 2 life.
 * IV — Create Galactus, a legendary 16/16 black Elder Alien creature token with flying,
 *      trample, and "Whenever Galactus attacks, destroy target land."
 *
 * Modeling notes:
 *  - "I, II"-style shared chapters are simply two `sagaChapter` blocks holding the same effect
 *    (Summon: Bahamut's shape) — each lore counter fires its own ability, so duplicating the
 *    effect is the faithful rendering, not a shortcut.
 *  - "Up to one target" is an *optional* target ([TargetPermanent] with `optional = true`), so
 *    chapter I can resolve with nothing chosen and never fizzles for lack of a legal permanent
 *    (a mandatory target would counter the chapter ability outright when the board is empty).
 *  - "Each opponent loses 2 life" is a plain life-loss aimed at `Player.EachOpponent` — *not* a
 *    drain, since the Saga gains its controller nothing.
 *  - Galactus is a *named* token with its own attack trigger, so chapter IV creates the
 *    registered `PredefinedTokens.Galactus` definition via [CreatePredefinedTokenEffect]
 *    rather than respelling a 16/16 inline (which would lose the destroy-a-land trigger).
 */
val TheComingOfGalactus = card("The Coming of Galactus") {
    manaCost = "{2}{B}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Destroy up to one target nonland permanent.\n" +
        "II, III — Each opponent loses 2 life.\n" +
        "IV — Create Galactus, a legendary 16/16 black Elder Alien creature token with flying, " +
        "trample, and \"Whenever Galactus attacks, destroy target land.\""

    // I — Destroy up to one target nonland permanent.
    sagaChapter(1) {
        val victim = target(
            "up to one target nonland permanent",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanent),
        )
        effect = Effects.Destroy(victim)
    }

    // II, III — Each opponent loses 2 life.
    sagaChapter(2) {
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }
    sagaChapter(3) {
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // IV — Create Galactus.
    sagaChapter(4) {
        effect = CreatePredefinedTokenEffect("Galactus")
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "212"
        artist = "Serena Malyon"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a38b92-619e-4fe8-b0cb-6fa31f0824ff.jpg?1784182991"
    }
}
