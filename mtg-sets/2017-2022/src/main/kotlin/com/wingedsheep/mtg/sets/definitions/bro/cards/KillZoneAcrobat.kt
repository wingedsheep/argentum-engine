package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kill-Zone Acrobat
 * {2}{B}
 * Creature — Human Soldier
 * 3/2
 * Whenever this creature attacks, you may sacrifice another creature or artifact. If you do,
 * this creature gains flying until end of turn.
 *
 * Same shape as Ashnod, Flesh Mechanist: an [Effects.MayPay] whose optional cost is the
 * sacrifice of another creature or artifact (`excludeSource`), gating the flying grant.
 */
val KillZoneAcrobat = card("Kill-Zone Acrobat") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, you may sacrifice another creature or artifact. " +
        "If you do, this creature gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.CreatureOrArtifact, excludeSource = true),
            then = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature or artifact. " +
            "If you do, this creature gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Justine Cruz"
        flavorText = "Her circus training proved surprisingly valuable on the battlefield."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48f97974-bb65-46b0-a710-12fa4b9343ac.jpg?1783920086"
    }
}
