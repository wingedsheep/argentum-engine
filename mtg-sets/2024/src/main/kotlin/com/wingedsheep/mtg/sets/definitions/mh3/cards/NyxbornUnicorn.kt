package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.dsl.mentorTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

val NyxbornUnicorn = card("Nyxborn Unicorn") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Unicorn"
    power = 2
    toughness = 2
    oracleText = "Bestow {3}{W} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\nMentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with lesser power.)\nEnchanted creature gets +2/+2 and has mentor."

    keywordAbility(KeywordAbility.bestow("{3}{W}"))
    mentor()

    staticAbility { ability = ModifyStats(2, 2) }
    staticAbility { ability = GrantKeyword(Keyword.MENTOR) }
    staticAbility { ability = GrantTriggeredAbility(mentorTriggeredAbility()) }

    metadata {
        ruling("2024-06-07", "Mentor compares the power of the creature with mentor with that of the target creature at two different times: once as the triggered ability is put onto the stack, and once as the triggered ability resolves. If you wish to raise a creature's power so its mentor ability can target a bigger creature, the last chance you have to do so is during the beginning of combat step.")
        ruling("2024-06-07", "If the target creature's power is no longer less than the attacking creature's power as the ability resolves, mentor doesn't add a +1/+1 counter. For example, if two 3/3 creatures with mentor attack and both mentor triggers target the same 2/2 creature, the first to resolve puts a +1/+1 counter on it and the second does nothing.")
        ruling("2024-06-07", "If the creature with mentor leaves the battlefield with the mentor ability on the stack, use its power as that creature last existed on the battlefield to determine whether the target creature has lesser power.")
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Josiah \"Jo\" Cameron"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa3a9f8f-b74f-44f1-a8b5-d21a55358a6c.jpg?1783911299"
    }
}
