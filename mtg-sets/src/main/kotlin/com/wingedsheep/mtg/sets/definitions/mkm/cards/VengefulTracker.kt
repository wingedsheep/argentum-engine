package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vengeful Tracker — Murders at Karlov Manor #147
 * {1}{R} · Creature — Human Detective · 2/2
 *
 * Whenever an opponent sacrifices an artifact, this creature deals 2 damage to them.
 *
 * The set is full of Clues, Treasures, and "murder weapon" Equipment that cash themselves in, so
 * this punishes the opponent for using their own artifacts, not just for edicts you point at them.
 *
 * "An artifact" is the bare-article template, so it is a per-permanent trigger (CR 603.2c): an
 * opponent cracking two Clues in response to one another gets hit twice, and an effect that
 * sacrifices three artifacts at once still fires three times. "An opponent" scopes it to the
 * Tracker controller's opponents ([Triggers.OpponentSacrificesA]) — your own Clue cracks never fire
 * it — and in a multiplayer game two opponents sacrificing in the same batch each trigger it, each
 * firing hitting the player who actually sacrificed via [Player.TriggeringPlayer].
 */
val VengefulTracker = card("Vengeful Tracker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Detective"
    power = 2
    toughness = 2
    oracleText = "Whenever an opponent sacrifices an artifact, this creature deals 2 damage to them."

    triggeredAbility {
        trigger = Triggers.OpponentSacrificesA(GameObjectFilter.Artifact)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent sacrifices an artifact, this creature deals 2 damage to them."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Francisco Miyara"
        flavorText = "\"I won't waste my time hunting down petty thieves while my brother's killer " +
            "still walks free.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a247c9a0-0c65-47bc-92fd-bebe95cd35a3.jpg?1783912872"
    }
}
