package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Feast of Worms
 * {3}{G}{G}
 * Sorcery — Arcane
 *
 * Destroy target land. If that land was legendary, its controller sacrifices another land of
 * their choice.
 *
 * "Was legendary" is read while the target is still on the battlefield (projected state), the
 * same shape as Molten Rain's "if that land was nonbasic". On the legendary branch the pipeline
 * gathers the target controller's *other* lands **before** the destruction — so "its controller"
 * is the player who controlled the land, not its owner, even if the land was stolen — then
 * destroys the target, and that controller ([Chooser.ControllerOfSelection]) picks one of the
 * gathered lands to sacrifice. `excludeChosenTargets` keeps the target itself out even when it
 * survives the destruction (indestructible / regenerated), which is what "another land" demands.
 */
val FeastOfWorms = card("Feast of Worms") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery — Arcane"
    oracleText = "Destroy target land. If that land was legendary, its controller sacrifices another land of their choice."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Land.legendary(), land),
            then = Effects.Pipeline(
                descriptionOverride = "Destroy target land. Its controller sacrifices another land of their choice."
            ) {
                val otherLands = gather(
                    CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Land,
                        player = Player.ControllerOf("target land"),
                        excludeChosenTargets = true
                    )
                )
                run(Effects.Destroy(land))
                val chosen = chooseExactly(
                    1,
                    from = otherLands,
                    chooser = Chooser.ControllerOfSelection,
                    prompt = "Choose a land to sacrifice",
                    useTargetingUI = true
                )
                sacrifice(chosen)
            },
            otherwise = Effects.Destroy(land)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Chippy"
        flavorText = "\"The dust beneath our feet was once part of a mighty civilization. Shall we too provide the path for a future generation?\"\n—Sensei Golden-Tail"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33119e6a-d69b-4039-add2-97fe35a89e8e.jpg?1783944290"
    }
}
