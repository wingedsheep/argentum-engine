package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dross Harvester — Mirrodin #63
 * {1}{B}{B} · Creature — Horror · 4/4
 *
 * Protection from white
 * At the beginning of your end step, you lose 4 life.
 * Whenever a creature dies, you gain 2 life.
 *
 * A 4/4 for three that bleeds you out unless the board keeps dying. The life-gain trigger is
 * [Triggers.AnyCreatureDies] — *any* creature, either player's, including Dross Harvester itself, so
 * the trigger has to be a battlefield-wide one rather than a self or you-control death trigger. Note
 * [Effects.LoseLife] defaults to targeting an opponent; the upkeep drain is explicitly
 * [EffectTarget.Controller].
 */
val DrossHarvester = card("Dross Harvester") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 4
    toughness = 4
    oracleText = "Protection from white\n" +
        "At the beginning of your end step, you lose 4 life.\n" +
        "Whenever a creature dies, you gain 2 life."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.LoseLife(4, EffectTarget.Controller)
    }

    triggeredAbility {
        trigger = Triggers.AnyCreatureDies
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b088832c-6119-4460-98b7-ae25cf70b2c5.jpg?1783944548"
    }
}
