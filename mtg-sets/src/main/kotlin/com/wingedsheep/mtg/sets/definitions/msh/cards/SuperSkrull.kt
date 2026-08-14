package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Super-Skrull — Marvel Super Heroes #115
 * {1}{B}{B}{B} · Legendary Creature — Skrull Shapeshifter Villain · Rare
 * 4/5
 *
 * Flying
 * {2}{W}: Create a 0/4 colorless Wall creature token with defender.
 * {3}{G}: Super-Skrull gets +4/+4 until end of turn.
 * {4}{R}: Super-Skrull deals 4 damage to target creature.
 * {5}{U}: Target player draws four cards.
 *
 * Four independent, unrestricted activated abilities — each a plain mana cost with no tap and no
 * activation limit, so any of them can be activated any number of times at instant speed as long as
 * the mana is there. The Fantastic Four homage means each ability is in a different color, but the
 * costs are ordinary mana costs; nothing about *paying* them cares which color paid for what.
 * Colour *identity* is a different question and does care — see [colorIdentity] below.
 *
 * The Wall token is colorless (empty `colors`), not white — the {W} in the activation cost doesn't
 * color the token. Its defender is a real keyword on the token, so the projected state stops it
 * attacking.
 */
val SuperSkrull = card("Super-Skrull") {
    manaCost = "{1}{B}{B}{B}"
    // WUBRG, not mono-black: colour identity counts the coloured pips in *every* mana symbol on the
    // card, activation costs included (CR 903.4), so the {W}/{G}/{R}/{U} activations all count. The
    // field is the authoritative Scryfall identity and feeds Commander deck validation, the
    // commander deck generator, and the draft/deckbuild scorers — "B" would let a mono-black
    // Commander deck run him illegally.
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Skrull Shapeshifter Villain"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "{2}{W}: Create a 0/4 colorless Wall creature token with defender.\n" +
        "{3}{G}: Super-Skrull gets +4/+4 until end of turn.\n" +
        "{4}{R}: Super-Skrull deals 4 damage to target creature.\n" +
        "{5}{U}: Target player draws four cards."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.CreateToken(
            power = 0,
            toughness = 4,
            creatureTypes = setOf("Wall"),
            keywords = setOf(Keyword.DEFENDER),
            imageUri = "https://cards.scryfall.io/normal/front/8/2/82d35a61-3c87-405d-b857-cf43067cb1c4.jpg?1783902804"
        )
        description = "{2}{W}: Create a 0/4 colorless Wall creature token with defender."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.ModifyStats(4, 4, EffectTarget.Self)
        description = "{3}{G}: Super-Skrull gets +4/+4 until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{R}")
        val creature = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(4, creature)
        description = "{4}{R}: Super-Skrull deals 4 damage to target creature."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        val player = target("target player", Targets.Player)
        effect = Effects.DrawCards(4, player)
        description = "{5}{U}: Target player draws four cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11fc8221-756a-4919-9272-38793e9c1ad9.jpg?1783902936"
    }
}
