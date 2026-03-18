import { chromium } from "playwright";

const browser = await chromium.launch({ headless: false, slowMo: 200 });
const page = await browser.newPage();
await page.setViewportSize({ width: 1440, height: 900 });
await page.goto("http://127.0.0.1:5173");
await page.waitForTimeout(2000);
await page.screenshot({ path: "E:/ai-workspaces/frontend-admin/screenshot-login.png" });
console.log("登录页截图完成，浏览器保持打开");
// 保持浏览器打开 60 秒供查看
await page.waitForTimeout(60000);
await browser.close();
