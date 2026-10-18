package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jukai Trainee — Kamigawa: Neon Dynasty #196
 * {1}{G} · Creature — Human Samurai · 2/2
 *
 * Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.
 *
 * Bushido 1 in all but name. The trigger has no partner clause, so it is the partner-less,
 * once-per-combat [Triggers.self.blocksOrBecomesBlocked] that Spitting Slug uses.
 */
val JukaiTrainee = card("Jukai Trainee") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Samurai"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Francisco Miyara"
        flavorText = "\"The Order of Jukai accepts only the strongest of body and spirit. I will train until I meet their expectations.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8be526cd-1480-4661-a05b-e004d7b4656c.jpg?1783923845"
    }
}
