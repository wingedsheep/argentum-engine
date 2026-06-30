package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Summon: G.F. Cerberus
 * {2}{R}{R}
 * Enchantment Creature — Saga Dog
 * 3/3
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Surveil 1.
 * II — Double — When you next cast an instant or sorcery spell this turn, copy it. You may choose
 *      new targets for the copy.
 * III — Triple — When you next cast an instant or sorcery spell this turn, copy it twice. You may
 *       choose new targets for the copies.
 *
 * Chapters II and III each install a one-shot "copy your next instant/sorcery this turn" delayed
 * trigger via [Effects.CopyNextSpellCast] — the same primitive Ether uses. The only difference
 * between them is the number of copies (1 vs 2); the copy machinery (CR 707.10c) pauses per copy
 * with targets so the controller may choose new targets, satisfying the "you may choose new
 * targets" clause for both. Each entry waits until a matching cast and is consumed once.
 */
val SummonGfCerberus = card("Summon: G.F. Cerberus") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Saga Dog"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n" +
        "II — Double — When you next cast an instant or sorcery spell this turn, copy it. You may choose new targets for the copy.\n" +
        "III — Triple — When you next cast an instant or sorcery spell this turn, copy it twice. You may choose new targets for the copies."
    power = 3
    toughness = 3

    sagaChapter(1) { effect = Effects.Surveil(1) }
    sagaChapter(2) { effect = Effects.CopyNextSpellCast(copies = 1) }
    sagaChapter(3) { effect = Effects.CopyNextSpellCast(copies = 2) }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Kevin Glint"
        flavorText = "\"Pretty confident. Let's see how you do.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0e5cbd4-401b-4456-80bf-d90beadfd1f8.jpg?1782686479"
    }
}
