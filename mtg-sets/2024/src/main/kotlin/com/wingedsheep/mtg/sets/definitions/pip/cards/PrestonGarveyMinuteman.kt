package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Preston Garvey, Minuteman — the Settlement is a predefined green Aura token (PredefinedTokens)
 * created attached via [Effects.CreateAuraToken]: an unchosen "up to one" target, or a land the
 * token can't legally enchant, creates no token (CR 303.4i). The attack trigger untaps every
 * enchanted permanent you control, whoever controls the Auras.
 */
val PrestonGarveyMinuteman = card("Preston Garvey, Minuteman") {
    manaCost = "{2}{R}{G}{W}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "At the beginning of combat on your turn, create a green Aura enchantment token named " +
        "Settlement attached to up to one target land you control. It has enchant land and " +
        "\"Enchanted land has '{T}: Add one mana of any color.'\"\n" +
        "Whenever Preston Garvey attacks, untap each enchanted permanent you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val land = target(TargetFilter.LandYouControl, optional = true)
        effect = Effects.CreateAuraToken("Settlement", land)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.untapGroup(GroupFilter(GameObjectFilter.Permanent.youControl().enchanted()))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Leonardo Borazio"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22b60168-1e7b-4c80-bb93-6841c1e961bb.jpg?1783912420"
        ruling(
            "2024-03-08",
            "If the target land is an illegal target as Preston Garvey's first ability tries to resolve, " +
                "it won't resolve and none of its effects happen. The Settlement token won't be created."
        )
        ruling(
            "2024-03-08",
            "In the rare case that the target land is a legal target as Preston Garvey's first ability " +
                "resolves but the target land can't legally be enchanted by the Settlement token, the token " +
                "won't be created at all."
        )
    }
}
