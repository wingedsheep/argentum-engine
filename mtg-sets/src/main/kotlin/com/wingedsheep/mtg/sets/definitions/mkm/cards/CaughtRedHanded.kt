package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Caught Red-Handed — Murders at Karlov Manor #115
 * {4}{R} · Instant
 *
 * This spell can't be countered. (This includes by the ward ability.)
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end
 * of turn. Suspect it.
 *
 * A Threaten at instant speed with two riders: it dodges counterspells and it leaves the borrowed
 * creature suspected, so once it goes back its controller can't block with it either.
 *
 * `cantBeCountered` stamps `CantBeCounteredComponent` on the spell (the Long Goodbye idiom), which
 * every counter path checks — ward included, since ward routes through
 * `StackResolver.counterSpell`. Targeting a warded creature still offers the ward cost; declining
 * leaves this on the stack to resolve anyway.
 *
 * The four clauses resolve in printed order against the single declared target: control change
 * (`Duration.EndOfTurn`, so it reverts in the cleanup step), untap, haste until end of turn, then
 * suspect. Suspect is deliberately **permanent** and not tied to the control change — per the
 * printed rulings a creature stays suspected until it leaves the battlefield or another effect
 * un-suspects it, so it is still suspected after control reverts. `Effects.Suspect` defaults to
 * `Duration.Permanent` for exactly that reason.
 */
val CaughtRedHanded = card("Caught Red-Handed") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell can't be countered. (This includes by the ward ability.)\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gains haste " +
        "until end of turn. Suspect it. (It has menace and can't block.)"

    cantBeCountered = true

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.GainControl(creature, Duration.EndOfTurn),
            Effects.Untap(creature),
            Effects.GrantKeyword(Keyword.HASTE, creature),
            Effects.Suspect(creature)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Donato Giancola"
        flavorText = "\"Wait, stop! I've been framed! Extremely convincingly!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95bc5f89-2f01-40c4-9883-4c90ab89fcbb.jpg?1783912886"

        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until " +
                "it leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
        ruling(
            "2024-02-02",
            "Being suspected isn't a copiable value. If a permanent becomes a copy of a suspected " +
                "creature, it won't be suspected."
        )
        ruling("2024-02-02", "If a creature is already suspected, suspecting it again won't have any effect.")
    }
}
