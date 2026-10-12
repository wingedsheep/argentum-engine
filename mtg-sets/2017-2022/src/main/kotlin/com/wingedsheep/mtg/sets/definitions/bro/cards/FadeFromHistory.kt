package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fade from History — The Brothers' War #177
 * {2}{G}{G} · Sorcery
 *
 * Each player who controls an artifact or enchantment creates a 2/2 green Bear creature token.
 * Then destroy all artifacts and enchantments.
 *
 * The No Witnesses shape: one [Effects.ForEachPlayer] over [Player.Each] whose body is an
 * [Effects.If]. Inside the loop the controller is rebound to the iterated player, so
 * [Conditions.YouControl] asks about *that* player and the Bear is created under their control.
 * Sequential iteration is safe because a plain Bear token is neither an artifact nor an
 * enchantment, so creating one can't change whether a later player qualifies. The wipe comes
 * after every Bear, so the check reads the pre-destruction battlefield, and the Bears survive it.
 */
val FadeFromHistory = card("Fade from History") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Each player who controls an artifact or enchantment creates a 2/2 green Bear " +
        "creature token. Then destroy all artifacts and enchantments."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.ArtifactOrEnchantment),
                then = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Bear"),
                    controller = EffectTarget.Controller,
                    imageUri = "https://cards.scryfall.io/normal/front/7/7/772dac39-269b-4a35-aad3-320279af833f.jpg?1783919910"
                ),
            ),
        ) then
            Effects.DestroyAll(GameObjectFilter.ArtifactOrEnchantment)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Alessandra Pisano"
        flavorText = "After a long hibernation, Argoth began slowly recovering from the ravages of war."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71f13f67-e852-4a6a-8f32-b16195e53ec3.jpg?1783920046"
    }
}
