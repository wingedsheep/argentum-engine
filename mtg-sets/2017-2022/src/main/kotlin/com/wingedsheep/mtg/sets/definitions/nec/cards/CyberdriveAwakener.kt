package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cyberdrive Awakener — Neon Dynasty Commander #12 (canonical printing)
 * {5}{U} · Artifact Creature — Construct · 4/4
 *
 * Flying
 * Other artifact creatures you control have flying.
 * When this creature enters, each noncreature artifact you control becomes a 4/4 artifact
 * creature until end of turn.
 *
 * The enters trigger is a one-shot [Effects.MassAnimate] over your noncreature artifacts: the
 * affected set is locked in on resolution, each becomes a creature with base 4/4 and keeps its
 * abilities (the card doesn't strip them). They were already artifacts, so only the creature type
 * is added — and the static lord then gives each of them flying for the turn.
 */
val CyberdriveAwakener = card("Cyberdrive Awakener") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Other artifact creatures you control have flying.\n" +
        "When this creature enters, each noncreature artifact you control becomes a 4/4 artifact " +
        "creature until end of turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()).other())
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MassAnimate(
            filter = GameObjectFilter.Artifact.notCreature().youControl(),
            power = DynamicAmounts.fixed(4),
            toughness = DynamicAmounts.fixed(4),
            loseAllAbilities = false,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a2c0a2f-07fb-4cc0-9b99-e5ff4303028d.jpg?1783923995"
    }
}
