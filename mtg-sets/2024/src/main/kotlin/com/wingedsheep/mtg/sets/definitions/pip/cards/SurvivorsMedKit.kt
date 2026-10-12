package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SurvivorsMedKit = card("Survivor's Med Kit") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Choose one that hasn't been chosen —\n• Stimpak — Draw a card.\n• Fancy Lads Snack Cakes — Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n• RadAway — Target player loses all rad counters. Sacrifice this artifact."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = ModalEffect.chooseOneNotYetChosen(
            Mode.noTarget(Effects.DrawCards(1), "Stimpak — Draw a card"),
            Mode.noTarget(Effects.CreateFood(), "Fancy Lads Snack Cakes — Create a Food token"),
            mode("RadAway — Remove all rad counters from target player, then sacrifice this artifact") {
                val player = target(Targets.Player)
                effect = Effects.RemoveAllCountersOfType(CounterType.RAD, player) then
                    Effects.SacrificeTarget(EffectTarget.Self)
            }
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2f30a39-aad3-45da-8f03-50b1d1806b00.jpg?1783912375"
    }
}
