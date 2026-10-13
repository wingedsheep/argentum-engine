package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elder Arthur Maxson — Fallout #102.
 *
 * Training is display-only as a keyword; the behaviour is its attack trigger. So the static grant
 * is two halves over the same group — the `TRAINING` badge and [trainingTriggeredAbility] — just as
 * Torens puts both on its token.
 */
val ElderArthurMaxson = card("Elder Arthur Maxson") {
    manaCost = "{1}{W}{B}"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 2
    oracleText = "Creature tokens you control have training. (Whenever a creature token you control attacks with another creature with greater power, put a +1/+1 counter on that token.)\nBlind Betrayal — Sacrifice another creature: Elder Arthur Maxson gains indestructible until end of turn."
    val creatureTokensYouControl = GroupFilter(GameObjectFilter.Creature.token().youControl())
    staticAbility { ability = GrantKeyword(Keyword.TRAINING, creatureTokensYouControl) }
    staticAbility { ability = GrantTriggeredAbility(trainingTriggeredAbility(), creatureTokensYouControl) }
    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/049771b0-69ad-4f2d-8567-47e8a87b4496.jpg?1783912391"
        ruling("2024-03-08", "A creature’s training ability triggers only when both that creature and a creature with greater power are declared as attackers. Increasing a creature’s power after attackers are declared won’t cause a training ability to trigger.")
        ruling("2024-03-08", "Once a creature’s training ability has triggered, destroying the other attacking creature or reducing its power won’t stop the creature with training from getting a +1/+1 counter.")
    }
}
