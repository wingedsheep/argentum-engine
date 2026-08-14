package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Barter in Blood — Mirrodin #57
 * {2}{B}{B} · Sorcery
 *
 * Each player sacrifices two creatures of their choice.
 *
 * The Deadly Brew / Rise of the Witch-king edict shape at `count = 2`: one
 * [Effects.Sacrifice] over [Player.Each]. Nothing is targeted — the spell is castable even
 * when no player controls a creature, and no player can be made an illegal "target".
 *
 * "Of their choice" is the executor's default: a player controlling more than two creatures is
 * prompted to pick exactly two, and a player controlling two or fewer sacrifices all of them
 * without a prompt (CR 608.2c does-as-much-as-it-can — the Scryfall ruling spells this out: a
 * player with a single creature sacrifices that one).
 *
 * Known deviation: the printed card has every player choose in APNAP order and then sacrifices
 * everything simultaneously. The engine walks the players one at a time, so a later player's
 * choice is made with the earlier sacrifices already resolved. This is the same approximation
 * the existing multi-player edicts ship with and only shows up when a sacrifice triggers
 * something that changes a later chooser's board.
 */
val BarterInBlood = card("Barter in Blood") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player sacrifices two creatures of their choice."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.Creature,
            count = 2,
            target = EffectTarget.PlayerRef(Player.Each)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Paolo Parente"
        flavorText = "\"In the game of conquest, who cares about the pawns if the king yet reigns?\"\n" +
            "—Geth, keeper of the Vault"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/beccbb2c-ca1d-4b72-9eca-a64a313fd830.jpg?1783944550"

        ruling(
            "2012-05-01",
            "The active player chooses which creatures will be sacrificed first, then each other " +
                "player in turn order does the same. Then all creatures are sacrificed simultaneously."
        )
        ruling("2012-05-01", "If a player controls only one creature, that creature is sacrificed.")
        ruling(
            "2012-05-01",
            "Barter in Blood doesn't target any creatures and may be cast even if a player " +
                "controls fewer than two creatures."
        )
    }
}
