package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirror Universe Intrusion
 * {1}{U}{B}
 * Sorcery
 * Kicker {4}{U}{B}
 * Create a token that's a copy of target creature an opponent controls, except it has menace. If
 * this spell was kicked, instead for each creature target opponent controls, create a token that's
 * a copy of that creature, except it has menace.
 *
 * The kicked branch replaces both the targeting and the effect (Fight with Fire's shape): kicked,
 * the spell targets only an opponent, and `ForEachInGroup` snapshots that player's creatures at
 * resolution, minting one menace copy per creature. (Scryfall's reminder text currently reads
 * "an additional 4"; the printed card says {4}{U}{B}.)
 */
val MirrorUniverseIntrusion = card("Mirror Universe Intrusion") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Kicker {4}{U}{B} (You may pay an additional {4}{U}{B} as you cast this spell.)\n" +
        "Create a token that's a copy of target creature an opponent controls, except it has menace. " +
        "If this spell was kicked, instead for each creature target opponent controls, create a token " +
        "that's a copy of that creature, except it has menace."

    keywordAbility(KeywordAbility.kicker("{4}{U}{B}"))

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CreateTokenCopyOfTarget(target = creature, addedKeywords = setOf(Keyword.MENACE))

        val opponent = kickerTarget(Targets.Opponent)
        kickerEffect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent)),
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.IterationEntity,
                addedKeywords = setOf(Keyword.MENACE)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "John Tyler Christopher"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3507f99b-15fb-4f66-9484-8e8115e7f5de.jpg?1792668526"
    }
}
