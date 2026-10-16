package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Structural Assault — Streets of New Capenna #126
 * {3}{R}{R} · Sorcery
 *
 * Destroy all artifacts, then Structural Assault deals damage to each creature equal to the number
 * of artifacts that were put into graveyards from the battlefield this turn.
 *
 * The count is the game-wide turn history (`TurnTracking(Player.Each, ARTIFACTS_DIED)`), not just
 * this spell's casualties. It is read when the damage step runs, after the wrath, so the artifacts
 * this spell destroyed already count ("then").
 */
val StructuralAssault = card("Structural Assault") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy all artifacts, then Structural Assault deals damage to each creature equal to " +
        "the number of artifacts that were put into graveyards from the battlefield this turn."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Artifact) then
            Patterns.Group.dealDamageToAll(DynamicAmounts.artifactsDiedThisTurn(), GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Liiga Smilshkalne"
        flavorText = "After the attack on the Vantoleone, the fragile balance of power shattered completely, " +
            "and the city descended into chaos."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbdb50e3-fe15-4431-b9bd-c4de65820734.jpg?1783923111"
        ruling("2022-04-29", "Structural Assault counts the number of artifacts that were put into graveyards from the battlefield this turn for any reason, not just because they were destroyed by Structural Assault.")
        ruling("2022-04-29", "If a player controls an artifact creature with a shield counter on it as Structural Assault destroys all artifacts, a shield counter will be removed before Structural Assault deals damage to each creature. Unless that artifact creature has another shield counter on it, the damage won't be prevented.")
    }
}
